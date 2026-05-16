package justfatlard.pinata.command;

import justfatlard.pinata.Pinata;
import justfatlard.pinata.block.PinataBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * /pinata spawn <config>
 *   Places a fully configured pinata where the player is looking.
 *
 * /pinata spawn <pos> <config>
 *   Places at explicit coordinates.
 *
 * Config format:
 *   hits=5 spread=2.0 indestructible cooldown=60 randomize contents=minecraft:diamond 5, minecraft:gold_ingot 3 | minecraft:emerald 10
 *
 * /pinata info <pos> - inspect an existing pinata
 */
public class PinataCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pinata")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                .then(Commands.literal("spawn")
                    // /pinata spawn <pos> <config>
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("config", StringArgumentType.greedyString())
                            .executes(ctx -> executeSpawn(ctx, BlockPosArgument.getBlockPos(ctx, "pos")))
                        )
                    )
                    // /pinata spawn <config> (at crosshair)
                    .then(Commands.argument("config", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            BlockPos pos = getTargetPos(ctx);
                            if (pos == null) return 0;
                            return executeSpawn(ctx, pos);
                        })
                    )
                )

                .then(Commands.literal("info")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(PinataCommand::executeInfo)
                    )
                )
        );
    }

    private static BlockPos getTargetPos(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command can only be used by a player."));
            return null;
        }

        HitResult hit = player.pick(5.0, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFailure(Component.literal("Look at a block to place the pinata."));
            return null;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        return blockHit.getBlockPos().relative(blockHit.getDirection());
    }

    private static int executeSpawn(CommandContext<CommandSourceStack> context, BlockPos pos) {
        try {
            ServerLevel world = context.getSource().getLevel();
            String config = StringArgumentType.getString(context, "config");

            int hits = 5;
            double spread = 0.5;
            int cooldown = 0;
            boolean indestructible = false;
            boolean randomize = false;
            List<List<PinataBlockEntity.ContentEntry>> contentSets = new ArrayList<>();

            String settingsPart;
            String contentsPart = null;
            int contentsIdx = config.indexOf("contents=");
            if (contentsIdx >= 0) {
                settingsPart = config.substring(0, contentsIdx).trim();
                contentsPart = config.substring(contentsIdx + "contents=".length()).trim();
            } else {
                settingsPart = config.trim();
            }

            for (String token : settingsPart.split("\\s+")) {
                if (token.isEmpty()) continue;

                if (token.equals("indestructible")) {
                    indestructible = true;
                } else if (token.equals("randomize")) {
                    randomize = true;
                } else if (token.startsWith("hits=")) {
                    hits = parseIntSetting(token, 5, 1, 1000);
                } else if (token.startsWith("spread=")) {
                    spread = parseDoubleSetting(token, 0.5, 0.0, 10.0);
                } else if (token.startsWith("cooldown=")) {
                    cooldown = parseIntSetting(token, 0, 0, 72000);
                }
            }

            if (contentsPart != null && !contentsPart.isEmpty()) {
                String[] sets = contentsPart.split("\\|");
                for (String set : sets) {
                    List<PinataBlockEntity.ContentEntry> entries = parseContentEntries(set.trim());
                    if (!entries.isEmpty()) {
                        contentSets.add(entries);
                    }
                }
            }

            world.setBlock(pos, Pinata.PINATA_BLOCK.defaultBlockState(), 3);

            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof PinataBlockEntity pinata)) {
                context.getSource().sendFailure(Component.literal("Failed to create pinata block entity."));
                return 0;
            }

            pinata.setHitsToBreak(hits);
            pinata.setSpreadDistance(spread);
            pinata.setCooldownTicks(cooldown);
            pinata.setIndestructible(indestructible);
            pinata.setRandomizeContentSets(randomize);
            for (List<PinataBlockEntity.ContentEntry> set : contentSets) {
                pinata.addContentSet(set);
            }

            StringBuilder sb = new StringBuilder("Spawned pinata at ");
            sb.append(pos.getX()).append(" ").append(pos.getY()).append(" ").append(pos.getZ());
            sb.append(" — hits=").append(hits);
            if (indestructible) sb.append(" indestructible");
            if (randomize) sb.append(" randomize");
            if (spread != 0.5) sb.append(" spread=").append(spread);
            if (cooldown > 0) sb.append(" cooldown=").append(cooldown);
            sb.append(" (").append(contentSets.size()).append(" content set(s))");

            String msg = sb.toString();
            context.getSource().sendSuccess(() -> Component.literal(msg), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        try {
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
            ServerLevel world = context.getSource().getLevel();
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof PinataBlockEntity pinata)) {
                context.getSource().sendFailure(Component.literal("No pinata block at that position."));
                return 0;
            }

            StringBuilder sb = new StringBuilder("Pinata Info:\n");
            sb.append("  Indestructible: ").append(pinata.isIndestructible()).append("\n");
            sb.append("  Hits to break: ").append(pinata.getHitsToBreak()).append("\n");
            sb.append("  Hits remaining: ").append(pinata.getHitsRemaining()).append("\n");
            sb.append("  Randomize sets: ").append(pinata.isRandomizeContentSets()).append("\n");
            sb.append("  Spread distance: ").append(pinata.getSpreadDistance()).append("\n");
            sb.append("  Cooldown: ").append(pinata.getCooldownTicks()).append(" ticks");
            if (pinata.getCooldownTicks() >= 20) sb.append(" (").append(pinata.getCooldownTicks() / 20.0).append("s)");
            sb.append("\n");
            sb.append("  Content sets: ").append(pinata.getContentSets().size()).append("\n");

            List<List<PinataBlockEntity.ContentEntry>> sets = pinata.getContentSets();
            for (int i = 0; i < sets.size(); i++) {
                sb.append("    Set ").append(i + 1);
                if (i == pinata.getCurrentContentSetIndex()) sb.append(" (current)");
                sb.append(": ");
                List<PinataBlockEntity.ContentEntry> set = sets.get(i);
                for (int j = 0; j < set.size(); j++) {
                    PinataBlockEntity.ContentEntry entry = set.get(j);
                    sb.append(entry.itemId()).append(" x").append(entry.count());
                    if (j < set.size() - 1) sb.append(", ");
                }
                sb.append("\n");
            }

            String msg = sb.toString();
            context.getSource().sendSuccess(() -> Component.literal(msg), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int parseIntSetting(String token, int defaultVal, int min, int max) {
        try {
            int val = Integer.parseInt(token.substring(token.indexOf('=') + 1));
            return Math.max(min, Math.min(max, val));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static double parseDoubleSetting(String token, double defaultVal, double min, double max) {
        try {
            double val = Double.parseDouble(token.substring(token.indexOf('=') + 1));
            return Math.max(min, Math.min(max, val));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static List<PinataBlockEntity.ContentEntry> parseContentEntries(String input) {
        List<PinataBlockEntity.ContentEntry> entries = new ArrayList<>();
        String[] parts = input.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            String[] tokens = trimmed.split("\\s+");
            if (tokens.length == 0) continue;

            String itemIdStr = tokens[0];
            int count = 1;
            if (tokens.length >= 2) {
                try {
                    count = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    // Default to 1
                }
            }

            Identifier itemId = Identifier.parse(itemIdStr);
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                entries.add(new PinataBlockEntity.ContentEntry(itemId, Math.max(1, Math.min(64, count))));
            }
        }

        return entries;
    }
}
