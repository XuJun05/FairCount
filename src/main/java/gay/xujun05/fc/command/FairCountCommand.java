package gay.xujun05.fc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import gay.xujun05.fc.Config;
import gay.xujun05.fc.FairCount;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;

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
                                    )
                            )
                            .then(Commands.literal("remove")
                                    .then(Commands.literal("all")
                                            .executes(FairCountCommand::modRemoveAll)
                                    )
                                    .then(Commands.argument("mod_id", StringArgumentType.string())
                                            .executes(FairCountCommand::modRemove)
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
            );
        });
    }

    // ========== Mod Commands ==========

    private static int modAdd(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "mod_id");
        if (Config.addMod(modId)) {
            context.getSource().sendSuccess(() -> Component.literal("§a[FairCount] Added mod to whitelist: " + modId), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c[FairCount] Mod is already in whitelist: " + modId));
            return 0;
        }
    }

    private static int modRemove(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "mod_id");
        if (Config.removeMod(modId)) {
            context.getSource().sendSuccess(() -> Component.literal("§a[FairCount] Removed mod from whitelist: " + modId), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c[FairCount] Mod is not in whitelist: " + modId));
            return 0;
        }
    }

    private static int modAddAll(CommandContext<CommandSourceStack> context) {
        int added = Config.addAllClientMods();
        if (added > 0) {
            context.getSource().sendSuccess(() -> Component.literal("§a[FairCount] Added " + added + " client mods to whitelist."), true);
        } else {
            context.getSource().sendFailure(Component.literal("§c[FairCount] No new client mods to add. Make sure players are connected."));
        }
        return added > 0 ? added : 1;
    }

    private static int modRemoveAll(CommandContext<CommandSourceStack> context) {
        int removed = Config.removeAllMods();
        context.getSource().sendSuccess(() -> Component.literal("§a[FairCount] Removed all " + removed + " mods from whitelist."), true);
        return 1;
    }

    private static int modList(CommandContext<CommandSourceStack> context) {
        List<String> mods = Config.getAllowedMods();
        if (mods.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§e[FairCount] Whitelist is empty."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§e[FairCount] Allowed mods (" + mods.size() + "):"), false);
            for (String mod : mods) {
                context.getSource().sendSuccess(() -> Component.literal("§7  - " + mod), false);
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
            context.getSource().sendFailure(Component.literal("§c[FairCount] Player not found: " + input + ". Use UUID or an online player name."));
            return 0;
        }

        if (Config.addPlayer(uuid)) {
            String displayText = input.equals(uuid) ? uuid : input + " (" + uuid + ")";
            context.getSource().sendSuccess(() -> Component.literal("§a[FairCount] Added player to ignored list: " + displayText), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c[FairCount] Player is already ignored: " + uuid));
            return 0;
        }
    }

    private static int playerRemove(CommandContext<CommandSourceStack> context) {
        String input = StringArgumentType.getString(context, "player");
        String uuid = resolvePlayerUuid(context, input);

        if (uuid == null) {
            context.getSource().sendFailure(Component.literal("§c[FairCount] Player not found: " + input + ". Use UUID or an online player name."));
            return 0;
        }

        if (Config.removePlayer(uuid)) {
            String displayText = input.equals(uuid) ? uuid : input + " (" + uuid + ")";
            context.getSource().sendSuccess(() -> Component.literal("§a[FairCount] Removed player from ignored list: " + displayText), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c[FairCount] Player is not in ignored list: " + uuid));
            return 0;
        }
    }

    private static int playerList(CommandContext<CommandSourceStack> context) {
        List<String> players = Config.getIgnoredPlayers();
        if (players.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§e[FairCount] Ignored player list is empty."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§e[FairCount] Ignored players (" + players.size() + "):"), false);
            for (String uuid : players) {
                // Try to resolve UUID to player name for display
                ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
                String display = onlinePlayer != null ? onlinePlayer.getName().getString() + " (" + uuid + ")" : uuid;
                context.getSource().sendSuccess(() -> Component.literal("§7  - " + display), false);
            }
        }
        return 1;
    }
}
