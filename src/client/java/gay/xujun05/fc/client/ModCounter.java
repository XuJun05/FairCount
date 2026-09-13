package gay.xujun05.fc.client;

import gay.xujun05.fc.networking.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class ModCounter {

    private final int pureJarCount;
    private final int nestedModCount;
    private final List<ModInfo> detectedMods;

    public ModCounter() {
        int pureCount = 0;
        int nestedCount = 0;
        List<ModInfo> mods = new ArrayList<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            ModOrigin origin = mod.getOrigin();
            String modId = mod.getMetadata().getId();
            boolean isNested = origin.getKind() == ModOrigin.Kind.NESTED;
            String sha256 = "";

            if (origin.getKind() == ModOrigin.Kind.PATH) {
                pureCount++;
                List<Path> paths = origin.getPaths();
                if (!paths.isEmpty()) {
                    sha256 = calculateSha256(paths.get(0));
                }
            } else if (isNested) {
                nestedCount++;
                System.out.println("[FairCount] Detected nested mod (JiJ): " + modId);
            }

            mods.add(new ModInfo(modId, sha256, isNested));
        }

        this.pureJarCount = pureCount;
        this.nestedModCount = nestedCount;
        this.detectedMods = mods;
    }

    public static String calculateSha256(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            System.err.println("[FairCount] Failed to calculate hash for " + path + ": " + e.getMessage());
            return "";
        }
    }

    public int getPureJarCount() { return pureJarCount; }
    public int getNestedModCount() { return nestedModCount; }
    public List<ModInfo> getDetectedMods() { return detectedMods; }
    public List<String> getDetectedModIds() {
        return detectedMods.stream().map(ModInfo::id).toList();
    }

    public void printSummary() {
        System.out.println("=========================================");
        System.out.println("[FairCount] User's pure JAR count (PATH): " + pureJarCount);
        System.out.println("[FairCount] Nested mod count (NESTED): " + nestedModCount);
        System.out.println("[FairCount] Total loaded Mod IDs count: " + detectedMods.size());
        System.out.println("=========================================");
    }
}