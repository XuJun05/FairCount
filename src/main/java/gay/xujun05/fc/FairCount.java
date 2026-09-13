package gay.xujun05.fc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import gay.xujun05.fc.networking.ModCheckPayload;
import gay.xujun05.fc.networking.ModInfo;
import gay.xujun05.fc.networking.ResourcePackInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FairCount implements ModInitializer {
    public static final String MOD_ID = "faircount";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<UUID> VERIFIED_PLAYERS = new HashSet<>();
    private static final Map<String, List<ModInfo>> CLIENT_MOD_LISTS = new HashMap<>();
    private static final Map<String, List<ResourcePackInfo>> CLIENT_RESOURCE_PACK_LISTS = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
        Config.load();
        gay.xujun05.fc.command.FairCountCommand.register();
        PayloadTypeRegistry.serverboundPlay().register(ModCheckPayload.TYPE, ModCheckPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ModCheckPayload.TYPE, (payload, context) -> {
            var player = context.player();
            var server = context.server();
            String playerName = player.getName().getString();
            UUID playerUuid = player.getUUID();

            int pureJar = payload.pureJarCount();
            int nested = payload.nestedCount();

            // Store client mod list for /faircount add all
            CLIENT_MOD_LISTS.put(playerName, new ArrayList<>(payload.mods()));

            // Store client resource pack list for /faircount pack add all
            List<ResourcePackInfo> clientPacks = payload.resourcePacks() != null ? payload.resourcePacks() : List.of();
            CLIENT_RESOURCE_PACK_LISTS.put(playerName, new ArrayList<>(clientPacks));

            List<String> unallowedMods = new ArrayList<>();
            List<String> mismatchedMods = new ArrayList<>();
            List<String> unallowedPacks = new ArrayList<>();
            List<String> mismatchedPacks = new ArrayList<>();

            for (ModInfo mod : payload.mods()) {
                String modId = mod.id();
                if (modId.startsWith("fabric-")) {
                    continue;
                }

                if (!Config.isModAllowed(modId)) {
                    unallowedMods.add(modId);
                } else if (!Config.isHashAllowed(modId, mod.sha256())) {
                    mismatchedMods.add(modId);
                    LOGGER.warn("[FairCount] Hash mismatch for mod '{}' from player {}. Received hash: '{}'", modId, playerName, mod.sha256());
                }
            }

            for (ResourcePackInfo pack : clientPacks) {
                String packName = pack.name();
                if (!Config.isResourcePackAllowed(packName)) {
                    unallowedPacks.add(packName);
                } else if (!Config.isResourcePackHashAllowed(packName, pack.sha256())) {
                    mismatchedPacks.add(packName);
                    LOGGER.warn("[FairCount] Hash mismatch for resource pack '{}' from player {}. Received hash: '{}'", packName, playerName, pack.sha256());
                }
            }

            LOGGER.info("=========================================");
            LOGGER.info("[FairCount] Inspection results for player [{}]", playerName);
            LOGGER.info("[FairCount] Pure JARs: {} | Nested Mods: {}", pureJar, nested);
            LOGGER.info("[FairCount] Unallowed mods: {} | Hash mismatches: {}", unallowedMods.size(), mismatchedMods.size());
            LOGGER.info("[FairCount] External Resource Packs: {} | Unallowed: {} | Hash mismatches: {}", clientPacks.size(), unallowedPacks.size(), mismatchedPacks.size());

            boolean isIgnored = Config.getIgnoredPlayers().contains(playerUuid.toString());
            boolean isOp = player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_OWNER);
            boolean bypass = isIgnored || isOp;

            boolean safe = (unallowedMods.isEmpty() && mismatchedMods.isEmpty() && unallowedPacks.isEmpty() && mismatchedPacks.isEmpty()) || bypass;

            if (safe) {
                if (bypass) {
                    LOGGER.info("[FairCount] Player {} ({}) bypassed checks (Ignored: {}, OP: {}).", playerName, playerUuid, isIgnored, isOp);
                } else {
                    LOGGER.info("[FairCount] No unauthorized mods or resource packs detected. Safe.");
                }
                VERIFIED_PLAYERS.add(playerUuid);
            } else {
                if (!unallowedMods.isEmpty()) {
                    LOGGER.warn("[FairCount] Detected unallowed mods: {}", unallowedMods);
                }
                if (!mismatchedMods.isEmpty()) {
                    LOGGER.warn("[FairCount] Detected modified/spoofed mods (hash mismatch): {}", mismatchedMods);
                }
                if (!unallowedPacks.isEmpty()) {
                    LOGGER.warn("[FairCount] Detected unallowed resource packs: {}", unallowedPacks);
                }
                if (!mismatchedPacks.isEmpty()) {
                    LOGGER.warn("[FairCount] Detected modified/spoofed resource packs (hash mismatch): {}", mismatchedPacks);
                }

                LOGGER.info("=========================================");

                server.execute(() -> {
                    if (!unallowedMods.isEmpty()) {
                        player.connection.disconnect(Localization.getComponent(
                                player, "faircount.kick.unallowed_mods", unallowedMods.toString()
                        ));
                    } else if (!mismatchedMods.isEmpty()) {
                        player.connection.disconnect(Localization.getComponent(
                                player, "faircount.kick.hash_mismatch", mismatchedMods.toString()
                        ));
                    } else if (!unallowedPacks.isEmpty()) {
                        player.connection.disconnect(Localization.getComponent(
                                player, "faircount.kick.unallowed_resource_pack", unallowedPacks.toString()
                        ));
                    } else {
                        player.connection.disconnect(Localization.getComponent(
                                player, "faircount.kick.resource_pack_hash_mismatch", mismatchedPacks.toString()
                        ));
                    }
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
                        boolean isIgnored = Config.getIgnoredPlayers().contains(playerUuid.toString());
                        boolean isOp = player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_OWNER);
                        if (isIgnored || isOp) {
                            LOGGER.info("[FairCount] Player {} ({}) bypassed timeout kick (Ignored: {}, OP: {}).", playerName, playerUuid, isIgnored, isOp);
                            return;
                        }
                        player.connection.disconnect(Localization.getComponent(
                                player, "faircount.kick.missing_mod"
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
            CLIENT_MOD_LISTS.remove(handler.getPlayer().getName().getString());
            CLIENT_RESOURCE_PACK_LISTS.remove(handler.getPlayer().getName().getString());
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Map<String, List<ModInfo>> getClientModLists() {
        return CLIENT_MOD_LISTS;
    }

    public static Map<String, List<ResourcePackInfo>> getClientResourcePackLists() {
        return CLIENT_RESOURCE_PACK_LISTS;
    }
}