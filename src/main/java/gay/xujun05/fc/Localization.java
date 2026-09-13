package gay.xujun05.fc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Localization {
    private static final Gson GSON = new Gson();
    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();
    private static final String[] SUPPORTED_LANGS = {"en_us", "ja_jp", "zh_cn"};

    // Hardcoded safety fallbacks in case lang files cannot be read from resources
    private static final Map<String, String> FALLBACK_EN = Map.of(
            "faircount.kick.missing_mod", "§c[FairCount]\nYou must install FairCount to join this server.\nPlease install the mod and reconnect.",
            "faircount.kick.unallowed_mods", "§c[FairCount]\nYou were kicked for having unallowed external mods: %s",
            "faircount.kick.hash_mismatch", "§c[FairCount]\nModified or unauthorized mod version detected (Hash mismatch): %s",
            "faircount.kick.unallowed_resource_pack", "§c[FairCount]\nYou were kicked for having unallowed resource packs: %s",
            "faircount.kick.resource_pack_hash_mismatch", "§c[FairCount]\nModified or unauthorized resource pack version detected (Hash mismatch): %s"
    );

    private static final Map<String, String> FALLBACK_JA = Map.of(
            "faircount.kick.missing_mod", "§c[FairCount]\nこのサーバーに参加するにはFairCountをインストールする必要があります。\nModをインストールして再接続してください。",
            "faircount.kick.unallowed_mods", "§c[FairCount]\n許可されていない外部Modが検出されたためキックされました: %s",
            "faircount.kick.hash_mismatch", "§c[FairCount]\n改ざんまたは未許可のバージョンのModが検出されました (Hash不一致): %s",
            "faircount.kick.unallowed_resource_pack", "§c[FairCount]\n許可されていない外部リソースパックが検出されたためキックされました: %s",
            "faircount.kick.resource_pack_hash_mismatch", "§c[FairCount]\n改ざんまたは未許可のリソースパックが検出されました (Hash不一致): %s"
    );

    private static final Map<String, String> FALLBACK_ZH = Map.of(
            "faircount.kick.missing_mod", "§c[FairCount]\n你必须安装FairCount才能加入此服务器。\n请安装该Mod后重新连接。",
            "faircount.kick.unallowed_mods", "§c[FairCount]\n你因为安装了不被允许的外部Mod而被踢出：%s",
            "faircount.kick.hash_mismatch", "§c[FairCount]\n检测到被修改或未经授权的Mod版本 (哈希值不匹配)：%s",
            "faircount.kick.unallowed_resource_pack", "§c[FairCount]\n你因为使用了不被允许的外部材质包/资源包而被踢出：%s",
            "faircount.kick.resource_pack_hash_mismatch", "§c[FairCount]\n检测到被修改或未经授权的材质包/资源包版本 (哈希值不匹配)：%s"
    );

    static {
        loadTranslations();
    }

    public static void loadTranslations() {
        TRANSLATIONS.clear();
        for (String lang : SUPPORTED_LANGS) {
            Map<String, String> map = loadLangFile(lang);
            if (map != null && !map.isEmpty()) {
                TRANSLATIONS.put(lang.toLowerCase(), map);
            }
        }
    }

    private static Map<String, String> loadLangFile(String lang) {
        String resourcePath = "assets/" + FairCount.MOD_ID + "/lang/" + lang + ".json";

        // 1. Try FabricLoader mod container
        try {
            Optional<Path> pathOpt = FabricLoader.getInstance().getModContainer(FairCount.MOD_ID)
                    .flatMap(c -> c.findPath(resourcePath));
            if (pathOpt.isPresent()) {
                try (BufferedReader reader = Files.newBufferedReader(pathOpt.get(), StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                }
            }
        } catch (Throwable ignored) {
        }

        // 2. Fallback to ClassLoader
        try (InputStream in = FairCount.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                }
            }
        } catch (Throwable ignored) {
        }

        // 3. Fallback to Class getResourceAsStream
        try (InputStream in = FairCount.class.getResourceAsStream("/" + resourcePath)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static String getLanguage(ServerPlayer player) {
        if (player != null) {
            try {
                var info = player.clientInformation();
                if (info != null && info.language() != null) {
                    return info.language().toLowerCase().replace("-", "_");
                }
            } catch (Throwable ignored) {
            }
        }
        return "en_us";
    }

    public static String translate(String lang, String key, Object... args) {
        String normalizedLang = (lang != null && !lang.isEmpty()) ? lang.toLowerCase().replace("-", "_") : "en_us";
        Map<String, String> langMap = TRANSLATIONS.get(normalizedLang);

        if (langMap == null || !langMap.containsKey(key)) {
            // Check prefix (e.g. ja from ja_jp, zh from zh_cn)
            String prefix = normalizedLang.contains("_") ? normalizedLang.split("_")[0] : normalizedLang;
            for (Map.Entry<String, Map<String, String>> entry : TRANSLATIONS.entrySet()) {
                if (entry.getKey().startsWith(prefix) && entry.getValue().containsKey(key)) {
                    langMap = entry.getValue();
                    break;
                }
            }
        }

        String template = null;
        if (langMap != null) {
            template = langMap.get(key);
        }

        // Fallback to en_us from loaded translations
        if (template == null) {
            Map<String, String> enMap = TRANSLATIONS.get("en_us");
            if (enMap != null) {
                template = enMap.get(key);
            }
        }

        // Fallback to hardcoded constants
        if (template == null) {
            if (normalizedLang.startsWith("ja")) {
                template = FALLBACK_JA.get(key);
            } else if (normalizedLang.startsWith("zh")) {
                template = FALLBACK_ZH.get(key);
            }
            if (template == null) {
                template = FALLBACK_EN.get(key);
            }
        }

        if (template == null) {
            template = key;
        }

        if (args != null && args.length > 0) {
            try {
                return String.format(template, args);
            } catch (Exception e) {
                return template;
            }
        }
        return template;
    }

    public static Component getComponent(ServerPlayer player, String key, Object... args) {
        String lang = getLanguage(player);
        return Component.literal(translate(lang, key, args));
    }
}
