package vn.haohan.metallurgy.guide;

import vn.haohan.metallurgy.recipe.MetallurgyRecipe;
import org.bukkit.Material;

import java.util.Objects;

/** Canonical two-chamber fuel combination read from the metallurgy config. */
public record FuelCombination(Material first, Material second, int maxTemperature) {
    public FuelCombination {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == Material.AIR || first == Material.CAVE_AIR || first == Material.VOID_AIR
                || second == Material.AIR || second == Material.CAVE_AIR || second == Material.VOID_AIR) {
            throw new IllegalArgumentException("fuel materials cannot be air");
        }
        if (maxTemperature < 0) {
            throw new IllegalArgumentException("maxTemperature cannot be negative");
        }
    }

    public boolean canStart(MetallurgyRecipe recipe) {
        return recipe != null && maxTemperature >= recipe.getMinTemperature();
    }
}
