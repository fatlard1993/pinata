package justfatlard.pinata.integration;

import java.util.List;
import justfatlard.pinata.Pinata;
import justfatlard.pinata.PinataParty;
import justfatlard.village_quests.api.DialogueRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Somebody in the village will sell you a party.
 *
 * <p>A pinata has no recipe at all. Until now the only way to get one was an
 * operator typing /pinata spawn, which means that on a server where nobody knows
 * the syntax the block may as well not exist. This is not a nudge toward
 * something you could have crafted; it is the only door in.
 *
 * <p>And nothing is sold. Reputation in this mod buys belonging rather than
 * goods, and a village that likes you enough throwing a party for the children
 * is belonging in its plainest form. Charging for it would turn the one moment
 * here that is purely warm into a transaction.
 *
 * <p>It also only comes up sometimes, which is the difference between an
 * occasion and a service.
 *
 */
public final class PinataDialogue {
	private PinataDialogue() {}

	private static final String OPTION_ID = "pinata:buy";

	/** High. This is the village deciding you are one of them, not a shop. */
	private static final int MIN_REPUTATION = 45;

	/** Sometimes. An offer that is always there stops being an occasion. */
	private static final float CHANCE = 0.15F;

	public static void register() {
		// Any profession: this is not specialist knowledge, it is a village that
		// likes an occasion.
		DialogueRegistry.registerUniversalDialogue((villager, player, reputation) -> {
			if (villager.level().getRandom().nextFloat() > CHANCE) return List.of();

			return List.of(new DialogueRegistry.DialogueOption(
				OPTION_ID,
				Component.literal("Does anything happen around here for fun?"),
				MIN_REPUTATION, Integer.MAX_VALUE));
		});

		DialogueRegistry.registerDialogueHandler(OPTION_ID, PinataDialogue::sell);
	}

	private static Component sell(net.minecraft.world.entity.npc.villager.Villager villager,
			ServerPlayer player, String optionId) {
		if (!(player.level() instanceof net.minecraft.server.level.ServerLevel world)) {
			return Component.literal("...Not just now.");
		}

		net.minecraft.core.BlockPos spot = PinataParty.findPartySpot(world, villager.blockPosition());
		if (spot == null) {
			return Component.literal("There is nowhere to hang it. Clear a bit of ground and ask me again.");
		}

		world.setBlockAndUpdate(spot, Pinata.PINATA_BLOCK.defaultBlockState());
		PinataParty.beginParty(world, spot);

		return Component.literal("Funny you should ask. Wait there. *goes inside, comes back with a sheep made of paper* "
			+ "Outside, where there is room. Give it a moment - they will have heard already, "
			+ "and they will not need telling twice.");
	}

}
