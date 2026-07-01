package gay.xujun05.fc.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.util.ArrayList;
import java.util.List;

public class ModCounter {

    private final int pureJarCount;
    private final int nestedModCount;
    private final List<String> detectedModIds;

    public ModCounter() {
        int pureCount = 0;
        int nestedCount = 0;
        List<String> modIds = new ArrayList<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            ModOrigin origin = mod.getOrigin();
            String modId = mod.getMetadata().getId();
            modIds.add(modId);

            if (origin.getKind() == ModOrigin.Kind.PATH) {
                pureCount++;
            }
            else if (origin.getKind() == ModOrigin.Kind.NESTED) {
                nestedCount++;
                System.out.println("[FairCount] Detected nested mod (JiJ): " + modId);
            }
        }

        this.pureJarCount = pureCount;
        this.nestedModCount = nestedCount;
        this.detectedModIds = modIds;
    }

    public int getPureJarCount() { return pureJarCount; }
    public int getNestedModCount() { return nestedModCount; }
    public List<String> getDetectedModIds() { return detectedModIds; }

    public void printSummary() {
        System.out.println("=========================================");
        System.out.println("[FairCount] User's pure JAR count (PATH): " + pureJarCount);
        System.out.println("[FairCount] Nested mod count (NESTED): " + nestedModCount);
        System.out.println("[FairCount] Total loaded Mod IDs count: " + detectedModIds.size());
        System.out.println("=========================================");
    }
}