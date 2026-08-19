package vn.haohan.metallurgy.guide;

import vn.haohan.metallurgy.recipe.MetallurgyRecipe;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetallurgyGuideDataTest {
    @Test
    void outputChanceDerivesComplementarySlagRates() {
        OutputChance chance = new OutputChance(0.35, 0.98, 1);

        assertEquals(35, chance.cleanWithoutBoraxPercent());
        assertEquals(65, chance.slagWithoutBoraxPercent());
        assertEquals(98, chance.cleanWithBoraxPercent());
        assertEquals(2, chance.slagWithBoraxPercent());
    }

    @Test
    void fuelCombinationChecksRecipeMinimumHeat() {
        MetallurgyRecipe recipe = new MetallurgyRecipe(
                "ancient_forge/iron_ingot", "ANCIENT_FORGE",
                List.of(new MetallurgyRecipe.Ingredient(Material.RAW_IRON, 1)),
                new MetallurgyRecipe.OutputItem(Material.IRON_INGOT, 1,
                        null, List.of(), 0, null),
                100, 10, 900, 1000, 1150, null,
                0.12, 0.80, List.of(), 1, -1.0, false, false);

        assertFalse(new FuelCombination(Material.COAL, Material.COAL, 850).canStart(recipe));
        assertTrue(new FuelCombination(Material.COAL, Material.CHARCOAL, 900).canStart(recipe));
    }

    @Test
    void categoryCollectionsAreImmutable() {
        MetallurgyCategory category = new MetallurgyCategory(
                "iron", "Iron", GuideIcon.vanilla(Material.RAW_IRON),
                MetallurgyCategory.Kind.METAL, List.of(), List.of());

        assertThrows(UnsupportedOperationException.class,
                () -> category.recipes().add(null));
    }
}
