package vn.haohan.metallurgy.recipe;

import vn.haohan.metallurgy.item.CustomItem;

import java.util.Locale;
import java.util.Optional;

/** Shared output-item mapping used by both machine logic and guide rendering. */
public final class MetallurgyOutputs {
    private MetallurgyOutputs() {}

    public static Optional<CustomItem> slagFor(MetallurgyRecipe recipe) {
        if (recipe == null) return Optional.empty();
        String key = (recipe.getId() + " " + String.valueOf(recipe.getOutput().customItemId()))
                .toLowerCase(Locale.ROOT);

        if (key.contains("mithril")) return Optional.of(CustomItem.MITHRIL_SLAG);
        if (key.contains("soulsteel")) return Optional.of(CustomItem.SOULSTEEL_SLAG);
        if (key.contains("embersteel")) return Optional.of(CustomItem.EMBERSTEEL_SLAG);
        if (key.contains("netherite") || key.contains("ancient_debris")) {
            return Optional.of(CustomItem.NETHERITE_SLAG);
        }
        if (key.contains("gold")) return Optional.of(CustomItem.GOLD_SLAG);
        if (key.contains("iron")) return Optional.of(CustomItem.IRON_SLAG);
        if (key.contains("copper")) return Optional.of(CustomItem.COPPER_SLAG);
        return CustomItem.getSlagForMaterial(recipe.getOutput().material());
    }
}
