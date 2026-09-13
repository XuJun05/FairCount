package gay.xujun05.fc.client;

import gay.xujun05.fc.networking.ResourcePackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

public class ResourcePackCounter {

    private static final Set<String> BUILT_IN_PACK_IDS = Set.of(
            "vanilla",
            "high_contrast",
            "programmer_art",
            "fabric"
    );

    private final List<ResourcePackInfo> detectedExternalPacks;

    public ResourcePackCounter() {
        this.detectedExternalPacks = new ArrayList<>();
        collectExternalPacks();
    }

    private void collectExternalPacks() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        PackRepository repository = client.getResourcePackRepository();
        if (repository == null) {
            return;
        }

        Path resourcePackDir = client.getResourcePackDirectory();

        for (Pack pack : repository.getSelectedPacks()) {
            String id = pack.getId();

            // Ignore built-in vanilla, feature, and mod internal packs
            if (isBuiltInPack(pack, id)) {
                continue;
            }

            String packFileName = id.startsWith("file/") ? id.substring(5) : id;
            Path packPath = resourcePackDir != null ? resourcePackDir.resolve(packFileName) : null;

            // If the pack file doesn't exist in resourcepacks directory, consider it an internal/virtual pack
            if (packPath == null || !Files.exists(packPath)) {
                continue;
            }

            String sha256 = "";
            if (Files.isRegularFile(packPath)) {
                sha256 = calculateFileSha256(packPath);
            } else if (Files.isDirectory(packPath)) {
                sha256 = calculateDirectorySha256(packPath);
            }

            String displayName = pack.getTitle() != null ? pack.getTitle().getString() : packFileName;
            detectedExternalPacks.add(new ResourcePackInfo(packFileName, displayName, sha256));
            System.out.println("[FairCount] Detected external resource pack: " + packFileName + " (hash: " + sha256 + ")");
        }
    }

    private boolean isBuiltInPack(Pack pack, String id) {
        if (BUILT_IN_PACK_IDS.contains(id)) {
            return true;
        }
        if (id.startsWith("fabric") || id.startsWith("fabric-") || id.startsWith("mod/")) {
            return true;
        }
        try {
            if (pack.getPackSource() == PackSource.BUILT_IN || pack.getPackSource() == PackSource.FEATURE) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static String calculateFileSha256(Path path) {
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
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            System.err.println("[FairCount] Failed to calculate hash for pack " + path + ": " + e.getMessage());
            return "";
        }
    }

    public static String calculateDirectorySha256(Path dir) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.walk(dir)) {
                stream.filter(Files::isRegularFile).sorted().forEach(p -> {
                    try {
                        byte[] relativePathBytes = dir.relativize(p).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8);
                        digest.update(relativePathBytes);
                        try (InputStream is = Files.newInputStream(p)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                digest.update(buffer, 0, read);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            System.err.println("[FairCount] Failed to calculate directory hash for " + dir + ": " + e.getMessage());
            return "";
        }
    }

    public List<ResourcePackInfo> getDetectedExternalPacks() {
        return detectedExternalPacks;
    }
}
