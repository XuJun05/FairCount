package gay.xujun05.fc;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final Logger LOGGER = FairCount.LOGGER;
    private static final List<String> ALLOWED_MODS = new ArrayList<>();

    private static final List<String> DEFAULT_MODS = List.of(
            "minecraft",
            "java",
            "fabricloader",
            "faircount",
            "fabric-api",
            "authme",
            "mixinextras"
    );

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve("faircount_whitelist.txt").toFile();

        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER && !configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                for (String modId : DEFAULT_MODS) {
                    writer.write(modId + "\n");
                }
                LOGGER.info("[FairCount] 初期コンフィグファイルを作成しました: {}", configFile.getName());
            } catch (IOException e) {
                LOGGER.error("[FairCount] コンフィグファイルの作成に失敗しました", e);
            }
        }

        ALLOWED_MODS.clear();
        if (configFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replace("\r", "").replace("\n", "").trim();

                    if (!line.isEmpty() && !line.startsWith("#")) {
                        ALLOWED_MODS.add(line);
                    }
                }
                LOGGER.info("[FairCount] コンフィグから {} 個の許可Modを読み込みました。", ALLOWED_MODS.size());
            } catch (IOException e) {
                LOGGER.error("[FairCount] コンフィグファイルの読み込みに失敗しました。デフォルト値を使用します。", e);
                ALLOWED_MODS.addAll(DEFAULT_MODS);
            }
        } else {
            ALLOWED_MODS.addAll(DEFAULT_MODS);
        }
    }

    public static List<String> getAllowedMods() {
        return ALLOWED_MODS;
    }
}