package justfatlard.pinata.integration;

import java.util.List;
import justfatlard.pinata.Pinata;
import justfatlard.village_quests.api.DialogueRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Somebody in the village will sell you a party.
 *
 * <p>A pinata is craftable and nobody crafts one, because nothing in the game
 * suggests such a thing exists and a recipe book only helps people already
 * looking. It is also the least serious object in the suite, which makes buying
 * one off a villager the right way to meet it: not a quest, not a lesson, just
 * a thing you can go and get if somebody tells you it is there.
 *
 * <p>Cheap on purpose. It is a party, not an heirloom, and the point is that a
 * child with eight emeralds can decide today is somebody's birthday.
 */
public final class PinataDialogue {
	private PinataDialogue() {}

	private static final String OPTION_ID = "pinata:buy";

	/** Low. Anybody who is not actively disliked can be sold a party. */
	private static final int MIN_REPUTATION = 15;
	private static final int PRICE = 8;

	public static void register() {
		// Any profession: this is not specialist knowledge, it is a village that
		// likes an occasion.
		DialogueRegistry.registerUniversalDialogue((villager, player, reputation) ->
			List.of(new DialogueRegistry.DialogueOption(
				OPTION_ID,
				Component.literal("Does anything happen around here for fun?"),
				MIN_REPUTATION, Integer.MAX_VALUE)));

		DialogueRegistry.registerDialogueHandler(OPTION_ID, PinataDialogue::sell);
	}

	private static Component sell(net.minecraft.world.entity.npc.villager.Villager villager,
			ServerPlayer player, String optionId) {
		if (countEmeralds(player) < PRICE) {
			return Component.literal("We hang a thing up and hit it until sweets come out. "
				+ PRICE + " emeralds and I will make you one. Do not ask me why it is a sheep.");
		}

		takeEmeralds(player);
		ItemStack pinata = new ItemStack(Pinata.PINATA_BLOCK_ITEM);
		if (!player.getInventory().add(pinata)) {
			player.drop(pinata, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}

		return Component.literal("Hang it somewhere with room to swing. "
			+ "And do not stand underneath it looking up, whatever anyone tells you.");
	}

	private static int countEmeralds(ServerPlayer player) {
		int found = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(Items.EMERALD)) found += stack.getCount();
		}
		return found;
	}

	private static void takeEmeralds(ServerPlayer player) {
		int remaining = PRICE;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (remaining <= 0) return;
			if (!stack.is(Items.EMERALD)) continue;

			int taken = Math.min(remaining, stack.getCount());
			stack.shrink(taken);
			remaining -= taken;
		}
	}
}
