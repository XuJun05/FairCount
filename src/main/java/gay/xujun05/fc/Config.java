package gay.xujun05.fc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gay.xujun05.fc.networking.ModInfo;
import gay.xujun05.fc.networking.ResourcePackInfo;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static final Logger LOGGER = FairCount.LOGGER;
    private static final Map<String, List<String>> ALLOWED_MODS = new LinkedHashMap<>();
    private static final Map<String, List<String>> ALLOWED_RESOURCE_PACKS = new LinkedHashMap<>();
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

    private static File getResourcePacksConfigFile() {
        return new File(getConfigDir(), "resource_packs.json");
    }

    public static void load() {
        ALLOWED_MODS.clear();
        IGNORED_PLAYERS.clear();
        ALLOWED_RESOURCE_PACKS.clear();

        loadMods();
        loadPlayers();
        loadResourcePacks();
    }

    private static void loadMods() {
        File modsFile = getModsConfigFile();

        if (modsFile.exists()) {
            try (FileReader reader = new FileReader(modsFile)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                if (jsonObject.has("allowed_mods")) {
                    var element = jsonObject.get("allowed_mods");
                    if (element.isJsonObject()) {
                        JsonObject modsObject = element.getAsJsonObject();
                        for (String modId : modsObject.keySet()) {
                            List<String> hashes = new ArrayList<>();
                            var hashElement = modsObject.get(modId);
                            if (hashElement.isJsonArray()) {
                                for (var h : hashElement.getAsJsonArray()) {
                                    String hashStr = h.getAsString().trim().toLowerCase();
                                    if (!hashes.contains(hashStr)) {
                                        hashes.add(hashStr);
                                    }
                                }
                            }
                            ALLOWED_MODS.put(modId, hashes);
                        }
                    } else if (element.isJsonArray()) {
                        // Migration from old array format
                        JsonArray modsArray = element.getAsJsonArray();
                        for (int i = 0; i < modsArray.size(); i++) {
                            ALLOWED_MODS.put(modsArray.get(i).getAsString(), new ArrayList<>());
                        }
                        saveMods();
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
                for (String modId : DEFAULT_MODS) {
                    ALLOWED_MODS.put(modId, new ArrayList<>());
                }
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
                    ALLOWED_MODS.put(modsArray.get(i).getAsString(), new ArrayList<>());
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
            for (String modId : DEFAULT_MODS) {
                ALLOWED_MODS.put(modId, new ArrayList<>());
            }
            saveMods();
            return;
        }
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new FileReader(oldConfig))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\r", "").replace("\n", "").trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    ALLOWED_MODS.put(line, new ArrayList<>());
                }
            }
            LOGGER.info("[FairCount] Loaded {} allowed mods from old config.", ALLOWED_MODS.size());
            saveMods();
        } catch (IOException e) {
            LOGGER.error("[FairCount] Failed to load old config file. Using default values.", e);
            for (String modId : DEFAULT_MODS) {
                ALLOWED_MODS.put(modId, new ArrayList<>());
            }
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

    public static void saveMods() {
        try (FileWriter writer = new FileWriter(getModsConfigFile())) {
            JsonObject jsonObject = new JsonObject();
            JsonObject modsObject = new JsonObject();
            for (Map.Entry<String, List<String>> entry : ALLOWED_MODS.entrySet()) {
                JsonArray hashArray = new JsonArray();
                for (String hash : entry.getValue()) {
                    hashArray.add(hash);
                }
                modsObject.add(entry.getKey(), hashArray);
            }
            jsonObject.add("allowed_mods", modsObject);
            GSON.toJson(jsonObject, writer);
            LOGGER.info("[FairCount] Saved mods config.");
        } catch (IOException e) {
            LOGGER.error("[FairCount] Failed to save mods config.", e);
        }
    }

    public static void savePlayers() {
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

    private static void loadResourcePacks() {
        File file = getResourcePacksConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                if (jsonObject.has("allowed_resource_packs")) {
                    var element = jsonObject.get("allowed_resource_packs");
                    if (element.isJsonObject()) {
                        JsonObject packsObject = element.getAsJsonObject();
                        for (String packName : packsObject.keySet()) {
                            List<String> hashes = new ArrayList<>();
                            var hashElement = packsObject.get(packName);
                            if (hashElement.isJsonArray()) {
                                for (var h : hashElement.getAsJsonArray()) {
                                    String hashStr = h.getAsString().trim().toLowerCase();
                                    if (!hashes.contains(hashStr)) {
                                        hashes.add(hashStr);
                                    }
                                }
                            }
                            ALLOWED_RESOURCE_PACKS.put(packName, hashes);
                        }
                    }
                }
                LOGGER.info("[FairCount] Loaded {} allowed resource packs from config.", ALLOWED_RESOURCE_PACKS.size());
            } catch (Exception e) {
                LOGGER.error("[FairCount] Failed to load resource packs config.", e);
            }
        } else {
            saveResourcePacks();
        }
    }

    public static void saveResourcePacks() {
        try (FileWriter writer = new FileWriter(getResourcePacksConfigFile())) {
            JsonObject jsonObject = new JsonObject();
            JsonObject packsObject = new JsonObject();
            for (Map.Entry<String, List<String>> entry : ALLOWED_RESOURCE_PACKS.entrySet()) {
                JsonArray hashArray = new JsonArray();
                for (String hash : entry.getValue()) {
                    hashArray.add(hash);
                }
                packsObject.add(entry.getKey(), hashArray);
            }
            jsonObject.add("allowed_resource_packs", packsObject);
            GSON.toJson(jsonObject, writer);
            LOGGER.info("[FairCount] Saved resource packs config.");
        } catch (IOException e) {
            LOGGER.error("[FairCount] Failed to save resource packs config.", e);
        }
    }

    public static Map<String, List<String>> getAllowedMods() {
        return ALLOWED_MODS;
    }

    public static List<String> getAllowedModIds() {
        return new ArrayList<>(ALLOWED_MODS.keySet());
    }

    public static boolean isModAllowed(String modId) {
        return ALLOWED_MODS.containsKey(modId);
    }

    /**
     * Checks if the given hash is accepted for this mod.
     * If no hashes are registered for this mod, or if sha256 is empty (virtual mod/nested), it is considered valid.
     */
    public static boolean isHashAllowed(String modId, String sha256) {
        List<String> hashes = ALLOWED_MODS.get(modId);
        if (hashes == null) {
            return false;
        }
        if (hashes.isEmpty() || sha256 == null || sha256.trim().isEmpty()) {
            return true;
        }
        String target = sha256.trim().toLowerCase();
        return hashes.stream().anyMatch(h -> h.equalsIgnoreCase(target));
    }

    public static boolean addMod(String modId) {
        if (!ALLOWED_MODS.containsKey(modId)) {
            ALLOWED_MODS.put(modId, new ArrayList<>());
            saveMods();
            return true;
        }
        return false;
    }

    public static boolean addMod(String modId, String sha256) {
        List<String> hashes = ALLOWED_MODS.computeIfAbsent(modId, k -> new ArrayList<>());
        String normalizedHash = sha256 != null ? sha256.trim().toLowerCase() : "";
        if (!normalizedHash.isEmpty() && hashes.stream().noneMatch(h -> h.equalsIgnoreCase(normalizedHash))) {
            hashes.add(normalizedHash);
            saveMods();
            return true;
        } else if (!ALLOWED_MODS.containsKey(modId)) {
            saveMods();
            return true;
        }
        return false;
    }

    public static boolean removeMod(String modId) {
        if (ALLOWED_MODS.remove(modId) != null) {
            saveMods();
            return true;
        }
        return false;
    }

    public static boolean removeModHash(String modId, String sha256) {
        List<String> hashes = ALLOWED_MODS.get(modId);
        if (hashes != null && sha256 != null) {
            String target = sha256.trim().toLowerCase();
            if (hashes.removeIf(h -> h.equalsIgnoreCase(target))) {
                saveMods();
                return true;
            }
        }
        return false;
    }

    public static int addAllClientMods() {
        int added = 0;
        for (List<ModInfo> modList : FairCount.getClientModLists().values()) {
            for (ModInfo mod : modList) {
                boolean isNew = !ALLOWED_MODS.containsKey(mod.id());
                List<String> hashes = ALLOWED_MODS.computeIfAbsent(mod.id(), k -> new ArrayList<>());
                String hash = mod.sha256() != null ? mod.sha256().trim().toLowerCase() : "";
                if (!hash.isEmpty()) {
                    if (hashes.stream().noneMatch(h -> h.equalsIgnoreCase(hash))) {
                        hashes.add(hash);
                        added++;
                    }
                } else if (isNew) {
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

    public static Map<String, List<String>> getAllowedResourcePacks() {
        return ALLOWED_RESOURCE_PACKS;
    }

    public static List<String> getAllowedResourcePackNames() {
        return new ArrayList<>(ALLOWED_RESOURCE_PACKS.keySet());
    }

    public static boolean isResourcePackAllowed(String packName) {
        if (packName == null) return false;
        String clean = packName.trim().toLowerCase();
        for (String allowed : ALLOWED_RESOURCE_PACKS.keySet()) {
            if (allowed.trim().equalsIgnoreCase(clean)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isResourcePackHashAllowed(String packName, String sha256) {
        if (packName == null) return false;
        String clean = packName.trim().toLowerCase();
        List<String> hashes = null;
        for (Map.Entry<String, List<String>> entry : ALLOWED_RESOURCE_PACKS.entrySet()) {
            if (entry.getKey().trim().equalsIgnoreCase(clean)) {
                hashes = entry.getValue();
                break;
            }
        }
        if (hashes == null) {
            return false;
        }
        if (hashes.isEmpty() || sha256 == null || sha256.trim().isEmpty()) {
            return true;
        }
        String target = sha256.trim().toLowerCase();
        return hashes.stream().anyMatch(h -> h.equalsIgnoreCase(target));
    }

    public static boolean addResourcePack(String packName) {
        return addResourcePack(packName, null);
    }

    public static boolean addResourcePack(String packName, String sha256) {
        if (packName == null || packName.trim().isEmpty()) return false;
        String trimmed = packName.trim();
        List<String> hashes = ALLOWED_RESOURCE_PACKS.computeIfAbsent(trimmed, k -> new ArrayList<>());
        String normalizedHash = sha256 != null ? sha256.trim().toLowerCase() : "";
        if (!normalizedHash.isEmpty() && hashes.stream().noneMatch(h -> h.equalsIgnoreCase(normalizedHash))) {
            hashes.add(normalizedHash);
            saveResourcePacks();
            return true;
        } else if (!ALLOWED_RESOURCE_PACKS.containsKey(trimmed)) {
            saveResourcePacks();
            return true;
        }
        return false;
    }

    public static boolean removeResourcePack(String packName) {
        if (packName == null) return false;
        String toRemove = null;
        for (String key : ALLOWED_RESOURCE_PACKS.keySet()) {
            if (key.equalsIgnoreCase(packName.trim())) {
                toRemove = key;
                break;
            }
        }
        if (toRemove != null && ALLOWED_RESOURCE_PACKS.remove(toRemove) != null) {
            saveResourcePacks();
            return true;
        }
        return false;
    }

    public static boolean removeResourcePackHash(String packName, String sha256) {
        if (packName == null || sha256 == null) return false;
        for (Map.Entry<String, List<String>> entry : ALLOWED_RESOURCE_PACKS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(packName.trim())) {
                String target = sha256.trim().toLowerCase();
                if (entry.getValue().removeIf(h -> h.equalsIgnoreCase(target))) {
                    saveResourcePacks();
                    return true;
                }
            }
        }
        return false;
    }

    public static int addAllClientResourcePacks() {
        int added = 0;
        for (List<ResourcePackInfo> packList : FairCount.getClientResourcePackLists().values()) {
            for (ResourcePackInfo pack : packList) {
                boolean isNew = !isResourcePackAllowed(pack.name());
                List<String> hashes = null;
                for (Map.Entry<String, List<String>> entry : ALLOWED_RESOURCE_PACKS.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(pack.name().trim())) {
                        hashes = entry.getValue();
                        break;
                    }
                }
                if (hashes == null) {
                    hashes = new ArrayList<>();
                    ALLOWED_RESOURCE_PACKS.put(pack.name().trim(), hashes);
                }
                String hash = pack.sha256() != null ? pack.sha256().trim().toLowerCase() : "";
                if (!hash.isEmpty()) {
                    if (hashes.stream().noneMatch(h -> h.equalsIgnoreCase(hash))) {
                        hashes.add(hash);
                        added++;
                    }
                } else if (isNew) {
                    added++;
                }
            }
        }
        if (added > 0) {
            saveResourcePacks();
        }
        return added;
    }

    public static int removeAllResourcePacks() {
        int removed = ALLOWED_RESOURCE_PACKS.size();
        ALLOWED_RESOURCE_PACKS.clear();
        saveResourcePacks();
        return removed;
    }
}