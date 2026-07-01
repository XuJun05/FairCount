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
            ModCounter activeCounter = new ModCounter();

            ModCheckPayload packet = new ModCheckPayload(
                    activeCounter.getPureJarCount(),
                    activeCounter.getNestedModCount(),
                    activeCounter.getDetectedModIds()
            );

            ClientPlayNetworking.send(packet);

            System.out.println("[FairCount] サーバーに接続しました。Mod数データを送信しました！");
        });
    }
}