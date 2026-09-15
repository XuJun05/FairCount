package gay.xujun05.fc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import gay.xujun05.fc.networking.ModCheckPayload;

public class FairCountClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModCounter counter = new ModCounter();
        counter.printSummary();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                ModCounter activeCounter = new ModCounter();
                ResourcePackCounter packCounter = new ResourcePackCounter();

                ModCheckPayload packet = new ModCheckPayload(
                        activeCounter.getPureJarCount(),
                        activeCounter.getNestedModCount(),
                        activeCounter.getDetectedMods(),
                        packCounter.getDetectedExternalPacks()
                );

                ClientPlayNetworking.send(packet);
                System.out.println("[FairCount] Connected to server. Sent mod count data!");
            } catch (Throwable t) {
                System.err.println("[FairCount] Error during JOIN mod inspection: " + t.getMessage());
                t.printStackTrace();
                try {
                    ModCounter fallbackCounter = new ModCounter();
                    ModCheckPayload fallbackPacket = new ModCheckPayload(
                            fallbackCounter.getPureJarCount(),
                            fallbackCounter.getNestedModCount(),
                            fallbackCounter.getDetectedMods(),
                            java.util.List.of()
                    );
                    ClientPlayNetworking.send(fallbackPacket);
                    System.out.println("[FairCount] Sent fallback mod count data.");
                } catch (Throwable fallbackError) {
                    System.err.println("[FairCount] Critical: Failed to send fallback payload: " + fallbackError.getMessage());
                }
            }
        });
    }
}