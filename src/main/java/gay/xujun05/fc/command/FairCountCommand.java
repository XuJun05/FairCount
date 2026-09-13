package gay.xujun05.fc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import gay.xujun05.fc.Config;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;
import java.util.Map;

public class FairCountCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("faircount")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))

                    // /faircount mod ...
                    .then(Commands.literal("mod")
                            .then(Commands.literal("add")
                                    .then(Commands.literal("all")
                                            .executes(FairCountCommand::modAddAll)
                                    )
                                    .then(Commands.argument("mod_id", StringArgumentType.string())
                                            .executes(FairCountCommand::modAdd)
                                            .then(Commands.argument("sha256", StringArgumentType.string())
                                                    .executes(FairCountCommand::modAddWithHash)
                                            )
                                    )
                            )
                            .then(Commands.literal("remove")
                                    .then(Commands.literal("all")
                                            .executes(FairCountCommand::modRemoveAll)
                                    )
                                    .then(Commands.argument("mod_id", StringArgumentType.string())
                                            .executes(FairCountCommand::modRemove)
                                            .then(Commands.argument("sha256", StringArgumentType.string())
                                                    .executes(FairCountCommand::modRemoveWithHash)
                                            )
                                    )
                            )
                            .then(Commands.literal("list")
                                    .executes(FairCountCommand::modList)
                            )
                    )

                    // /faircount player ...
                    .then(Commands.literal("player")
                            .then(Commands.literal("add")
                                    .then(Commands.argument("player", StringArgumentType.string())
                                            .executes(FairCountCommand::playerAdd)
                                    )
                            )
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("player", StringArgumentType.string())
                                            .executes(FairCountCommand::playerRemove)
                                    )
                            )
                            .then(Commands.literal("list")
                                    .executes(FairCountCommand::playerList)
                            )
                    )

                    // /faircount pack ...
                    .then(Commands.literal("pack")
                            .then(Commands.literal("add")
                                    .then(Commands.literal("all")
                                            .executes(FairCountCommand::packAddAll)
                                    )
                                    .then(Commands.argument("pack_name", StringArgumentType.string())
                                            .executes(FairCountCommand::packAdd)
                                            .then(Commands.argument("sha256", StringArgumentType.string())
                                                    .executes(FairCountCommand::packAddWithHash)
                                            )
                                    )
                            )
                            .then(Commands.literal("remove")
                                    .then(Commands.literal("all")
                                            .executes(FairCountCommand::packRemoveAll)
                                    )
                                    .then(Commands.argument("pack_name", StringArgumentType.string())
                                            .executes(FairCountCommand::packRemove)
                                            .then(Commands.argument("sha256", StringArgumentType.string())
                                                    .executes(FairCountCommand::packRemoveWithHash)
                                            )
                                    )
                            )
                            .then(Commands.literal("list")
                                    .executes(FairCountCommand::packList)
                            )
                    )
            );
        });
    }

    // ========== Mod Commands ==========

    private static int modAdd(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "mod_id");
        if (Config.addMod(modId)) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.add.success", modId), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.mod.add.fail", modId));
            return 0;
        }
    }

    private static int modAddWithHash(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "mod_id");
        String hash = StringArgumentType.getString(context, "sha256").toLowerCase();
        if (Config.addMod(modId, hash)) {
            String shortHash = hash.substring(0, Math.min(8, hash.length())) + "...";
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.add_hash.success", modId, shortHash), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.mod.add_hash.fail", modId));
            return 0;
        }
    }

    private static int modRemove(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "mod_id");
        if (Config.removeMod(modId)) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.remove.success", modId), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.mod.remove.fail", modId));
            return 0;
        }
    }

    private static int modRemoveWithHash(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "mod_id");
        String hash = StringArgumentType.getString(context, "sha256").toLowerCase();
        if (Config.removeModHash(modId, hash)) {
            String shortHash = hash.substring(0, Math.min(8, hash.length())) + "...";
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.remove_hash.success", modId, shortHash), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.mod.remove_hash.fail", modId));
            return 0;
        }
    }

    private static int modAddAll(CommandContext<CommandSourceStack> context) {
        int added = Config.addAllClientMods();
        if (added > 0) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.add_all.success", added), true);
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.mod.add_all.fail"));
        }
        return added > 0 ? added : 1;
    }

    private static int modRemoveAll(CommandContext<CommandSourceStack> context) {
        int removed = Config.removeAllMods();
        context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.remove_all.success", removed), true);
        return 1;
    }

    private static int modList(CommandContext<CommandSourceStack> context) {
        Map<String, List<String>> mods = Config.getAllowedMods();
        if (mods.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.list.empty"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.mod.list.header", mods.size()), false);
            for (Map.Entry<String, List<String>> entry : mods.entrySet()) {
                String modId = entry.getKey();
                List<String> hashes = entry.getValue();
                String info = hashes.isEmpty() ? "(all versions)" : "(" + hashes.size() + " hashes)";
                context.getSource().sendSuccess(() -> Component.translatable("faircount.command.list.item", modId + " " + info), false);
            }
        }
        return 1;
    }

    // ========== Player Commands ==========

    private static String resolvePlayerUuid(CommandContext<CommandSourceStack> context, String input) {
        // Check if it's already a UUID format
        if (input.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return input;
        }

        // Try to resolve as an online player name
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(input)) {
                return player.getUUID().toString();
            }
        }

        return null;
    }

    private static int playerAdd(CommandContext<CommandSourceStack> context) {
        String input = StringArgumentType.getString(context, "player");
        String uuid = resolvePlayerUuid(context, input);

        if (uuid == null) {
            context.getSource().sendFailure(Component.translatable("faircount.command.player.not_found", input));
            return 0;
        }

        if (Config.addPlayer(uuid)) {
            String displayText = input.equals(uuid) ? uuid : input + " (" + uuid + ")";
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.player.add.success", displayText), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.player.add.fail", uuid));
            return 0;
        }
    }

    private static int playerRemove(CommandContext<CommandSourceStack> context) {
        String input = StringArgumentType.getString(context, "player");
        String uuid = resolvePlayerUuid(context, input);

        if (uuid == null) {
            context.getSource().sendFailure(Component.translatable("faircount.command.player.not_found", input));
            return 0;
        }

        if (Config.removePlayer(uuid)) {
            String displayText = input.equals(uuid) ? uuid : input + " (" + uuid + ")";
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.player.remove.success", displayText), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.player.remove.fail", uuid));
            return 0;
        }
    }

    private static int playerList(CommandContext<CommandSourceStack> context) {
        List<String> players = Config.getIgnoredPlayers();
        if (players.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.player.list.empty"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.player.list.header", players.size()), false);
            for (String uuid : players) {
                // Try to resolve UUID to player name for display
                ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
                String display = onlinePlayer != null ? onlinePlayer.getName().getString() + " (" + uuid + ")" : uuid;
                context.getSource().sendSuccess(() -> Component.translatable("faircount.command.list.item", display), false);
            }
        }
        return 1;
    }

    // ========== Resource Pack Commands ==========

    private static int packAdd(CommandContext<CommandSourceStack> context) {
        String packName = StringArgumentType.getString(context, "pack_name");
        if (Config.addResourcePack(packName)) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.add.success", packName), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.pack.add.fail", packName));
            return 0;
        }
    }

    private static int packAddWithHash(CommandContext<CommandSourceStack> context) {
        String packName = StringArgumentType.getString(context, "pack_name");
        String hash = StringArgumentType.getString(context, "sha256").toLowerCase();
        if (Config.addResourcePack(packName, hash)) {
            String shortHash = hash.substring(0, Math.min(8, hash.length())) + "...";
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.add_hash.success", packName, shortHash), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.pack.add_hash.fail", packName));
            return 0;
        }
    }

    private static int packRemove(CommandContext<CommandSourceStack> context) {
        String packName = StringArgumentType.getString(context, "pack_name");
        if (Config.removeResourcePack(packName)) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.remove.success", packName), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.pack.remove.fail", packName));
            return 0;
        }
    }

    private static int packRemoveWithHash(CommandContext<CommandSourceStack> context) {
        String packName = StringArgumentType.getString(context, "pack_name");
        String hash = StringArgumentType.getString(context, "sha256").toLowerCase();
        if (Config.removeResourcePackHash(packName, hash)) {
            String shortHash = hash.substring(0, Math.min(8, hash.length())) + "...";
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.remove_hash.success", packName, shortHash), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.pack.remove_hash.fail", packName));
            return 0;
        }
    }

    private static int packAddAll(CommandContext<CommandSourceStack> context) {
        int added = Config.addAllClientResourcePacks();
        if (added > 0) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.add_all.success", added), true);
        } else {
            context.getSource().sendFailure(Component.translatable("faircount.command.pack.add_all.fail"));
        }
        return added > 0 ? added : 1;
    }

    private static int packRemoveAll(CommandContext<CommandSourceStack> context) {
        int removed = Config.removeAllResourcePacks();
        context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.remove_all.success", removed), true);
        return 1;
    }

    private static int packList(CommandContext<CommandSourceStack> context) {
        Map<String, List<String>> packs = Config.getAllowedResourcePacks();
        if (packs.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.list.empty"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("faircount.command.pack.list.header", packs.size()), false);
            for (Map.Entry<String, List<String>> entry : packs.entrySet()) {
                String packName = entry.getKey();
                List<String> hashes = entry.getValue();
                String info = hashes.isEmpty() ? "§7(no hash)" : "§8(" + hashes.size() + " hash" + (hashes.size() > 1 ? "es" : "") + ")";
                context.getSource().sendSuccess(() -> Component.translatable("faircount.command.list.item", packName + " " + info), false);
            }
        }
        return 1;
    }
}
