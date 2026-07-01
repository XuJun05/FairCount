package gay.xujun05.fc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import gay.xujun05.fc.networking.ModCheckPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FairCount implements ModInitializer {
    public static final String MOD_ID = "faircount";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<UUID> VERIFIED_PLAYERS = new HashSet<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
        Config.load();
        PayloadTypeRegistry.serverboundPlay().register(ModCheckPayload.TYPE, ModCheckPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ModCheckPayload.TYPE, (payload, context) -> {
            var player = context.player();
            var server = context.server();
            String playerName = player.getName().getString();
            UUID playerUuid = player.getUUID();

            int pureJar = payload.pureJarCount();
            int nested = payload.nestedCount();

            List<String> extraMods = new ArrayList<>(payload.modIds());
            extraMods.removeAll(Config.getAllowedMods());
            extraMods.removeIf(id -> id.startsWith("fabric-"));

            LOGGER.info("=========================================");
            LOGGER.info("[FairCount] Mod inspection results for player [{}]", playerName);
            LOGGER.info("[FairCount] Pure JARs: {} | Nested Mods: {}", pureJar, nested);
            LOGGER.info("[FairCount] Unallowed mods: {}", extraMods.size());

            if (extraMods.isEmpty()) {
                LOGGER.info("[FairCount] No extra mods detected. Safe.");
                VERIFIED_PLAYERS.add(playerUuid);
            } else {
                LOGGER.warn("[FairCount] Detected unallowed mods: {}", extraMods);

                LOGGER.info("=========================================");

                server.execute(() -> {
                    player.connection.disconnect(Component.literal(
                            "§c[FairCount]\nYou were kicked for having unallowed external mods: " + extraMods
                    ));
                });
                return;
            }
            LOGGER.info("=========================================");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID playerUuid = player.getUUID();
            String playerName = player.getName().getString();

            VERIFIED_PLAYERS.remove(playerUuid);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                server.execute(() -> {
                    if (server.getPlayerList().getPlayer(playerUuid) != null && !VERIFIED_PLAYERS.contains(playerUuid)) {
                        player.connection.disconnect(Component.literal(
                                "§c[FairCount]\nYou must install FairCount to join this server.\n" +
                                        "Please install the mod and reconnect."
                        ));
                        LOGGER.info("=========================================");
                        LOGGER.warn("[FairCount] Player {} was kicked for not sending the packet, assuming mod is not installed.", playerName);
                        LOGGER.info("=========================================");
                    }
                });
            }).start();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            VERIFIED_PLAYERS.remove(handler.getPlayer().getUUID());
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}