package gay.xujun05.fc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final Logger LOGGER = FairCount.LOGGER;
    private static final List<String> ALLOWED_MODS = new ArrayList<>();
    private static final List<String> IGNORED_PLAYERS = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<String> DEFAULT_MODS = List.of(
            "minecraft",
            "java",
            "fabricloader",
            "faircount",
            "fabric-api",
            "mixinextras"
    );

    private static File getConfigDir() {
        File dir = FabricLoader.getInstance().getConfigDir().resolve("faircount").toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static File getModsConfigFile() {
        return new File(getConfigDir(), "mods.json");
    }

    private static File getPlayersConfigFile() {
        return new File(getConfigDir(), "players.json");
    }

    public static void load() {
        ALLOWED_MODS.clear();
        IGNORED_PLAYERS.clear();

        loadMods();
        loadPlayers();
    }

    private static void loadMods() {
        File modsFile = getModsConfigFile();

        if (modsFile.exists()) {
            try (FileReader reader = new FileReader(modsFile)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                if (jsonObject.has("allowed_mods")) {
                    JsonArray modsArray = jsonObject.getAsJsonArray("allowed_mods");
                    for (int i = 0; i < modsArray.size(); i++) {
                        ALLOWED_MODS.add(modsArray.get(i).getAsString());
                    }
                }
                LOGGER.info("[FairCount] Loaded {} allowed mods from config.", ALLOWED_MODS.size());
            } catch (Exception e) {
                LOGGER.error("[FairCount] Failed to load mods config. Trying old format.", e);
                loadOldFormat();
            }
        } else {
            File oldConfig = FabricLoader.getInstance().getConfigDir().resolve("faircount_whitelist.json").toFile();
            if (oldConfig.exists()) {
                migrateOldConfig(oldConfig);
            } else {
                ALLOWED_MODS.addAll(DEFAULT_MODS);
                saveMods();
            }
        }
    }

    private static void migrateOldConfig(File oldConfig) {
        try (FileReader reader = new FileReader(oldConfig)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (jsonObject.has("allowed_mods")) {
                JsonArray modsArray = jsonObject.getAsJsonArray("allowed_mods");
                for (int i = 0; i < modsArray.size(); i++) {
                    ALLOWED_MODS.add(modsArray.get(i).getAsString());
                }
            }
            if (jsonObject.has("ignored_players")) {
                JsonArray playersArray = jsonObject.getAsJsonArray("ignored_players");
                for (int i = 0; i < playersArray.size(); i++) {
                    IGNORED_PLAYERS.add(playersArray.get(i).getAsString());
                }
            }
            LOGGER.info("[FairCount] Migrated old config to new format.");
            saveMods();
            savePlayers();
        } catch (Exception e) {
            LOGGER.error("[FairCount] Failed to migrate old config.", e);
            loadOldFormat();
        }
    }

    private static void loadOldFormat() {
        File oldConfig = FabricLoader.getInstance().getConfigDir().resolve("faircount_whitelist.json").toFile();
        if (!oldConfig.exists()) {
            ALLOWED_MODS.addAll(DEFAULT_MODS);
            saveMods();
            return;
        }
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new FileReader(oldConfig))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\r", "").replace("\n", "").trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    ALLOWED_MODS.add(line);
                }
            }
            LOGGER.info("[FairCount] Loaded {} allowed mods from old config.", ALLOWED_MODS.size());
            saveMods();
        } catch (IOException e) {
            LOGGER.error("[FairCount] Failed to load old config file. Using default values.", e);
            ALLOWED_MODS.addAll(DEFAULT_MODS);
            saveMods();
        }
    }

    private static void loadPlayers() {
        File playersFile = getPlayersConfigFile();

        if (playersFile.exists()) {
            try (FileReader reader = new FileReader(playersFile)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                boolean needsCleanup = false;
                if (jsonObject.has("ignored_players")) {
                    JsonArray playersArray = jsonObject.getAsJsonArray("ignored_players");
                    for (int i = 0; i < playersArray.size(); i++) {
                        String entry = playersArray.get(i).getAsString();
                        if (entry.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                            IGNORED_PLAYERS.add(entry);
                        } else {
                            LOGGER.warn("[FairCount] Skipping invalid player entry (not a UUID): {}", entry);
                            needsCleanup = true;
                        }
                    }
                }
                LOGGER.info("[FairCount] Loaded {} ignored players from config.", IGNORED_PLAYERS.size());
                if (needsCleanup) {
                    savePlayers();
                }
            } catch (Exception e) {
                LOGGER.error("[FairCount] Failed to load players config.", e);
            }
        } else {
            savePlayers();
        }
    }

    private static void saveMods() {
        try (FileWriter writer = new FileWriter(getModsConfigFile())) {
            JsonObject jsonObject = new JsonObject();
            JsonArray modsArray = new JsonArray();
            for (String modId : ALLOWED_MODS) {
                modsArray.add(modId);
            }
            jsonObject.add("allowed_mods", modsArray);
            GSON.toJson(jsonObject, writer);
            LOGGER.info("[FairCount] Saved mods config.");
        } catch (IOException e) {
            LOGGER.error("[FairCount] Failed to save mods config.", e);
        }
    }

    private static void savePlayers() {
        try (FileWriter writer = new FileWriter(getPlayersConfigFile())) {
            JsonObject jsonObject = new JsonObject();
            JsonArray playersArray = new JsonArray();
            for (String playerUuid : IGNORED_PLAYERS) {
                playersArray.add(playerUuid);
            }
            jsonObject.add("ignored_players", playersArray);
            GSON.toJson(jsonObject, writer);
            LOGGER.info("[FairCount] Saved players config.");
        } catch (IOException e) {
            LOGGER.error("[FairCount] Failed to save players config.", e);
        }
    }

    public static List<String> getAllowedMods() {
        return ALLOWED_MODS;
    }

    public static boolean addMod(String modId) {
        if (!ALLOWED_MODS.contains(modId)) {
            ALLOWED_MODS.add(modId);
            saveMods();
            return true;
        }
        return false;
    }

    public static boolean removeMod(String modId) {
        if (ALLOWED_MODS.remove(modId)) {
            saveMods();
            return true;
        }
        return false;
    }

    public static int addAllClientMods() {
        int added = 0;
        for (List<String> modList : FairCount.getClientModLists().values()) {
            for (String modId : modList) {
                if (!ALLOWED_MODS.contains(modId)) {
                    ALLOWED_MODS.add(modId);
                    added++;
                }
            }
        }
        if (added > 0) {
            saveMods();
        }
        return added;
    }

    public static int removeAllMods() {
        int removed = ALLOWED_MODS.size();
        ALLOWED_MODS.clear();
        saveMods();
        return removed;
    }

    public static List<String> getIgnoredPlayers() {
        return IGNORED_PLAYERS;
    }

    public static boolean addPlayer(String playerUuid) {
        // Only accept valid UUID format
        if (!playerUuid.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return false;
        }
        if (!IGNORED_PLAYERS.contains(playerUuid)) {
            IGNORED_PLAYERS.add(playerUuid);
            savePlayers();
            return true;
        }
        return false;
    }

    public static boolean removePlayer(String playerUuid) {
        if (IGNORED_PLAYERS.remove(playerUuid)) {
            savePlayers();
            return true;
        }
        return false;
    }
}