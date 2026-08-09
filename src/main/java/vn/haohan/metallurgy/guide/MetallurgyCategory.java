package vn.haohan.metallurgy.guide;

import vn.haohan.metallurgy.recipe.MetallurgyRecipe;

import java.util.List;
import java.util.Objects;

/** Immutable, renderer-agnostic category consumed by the Ancient Forge guide. */
public record MetallurgyCategory(
        String id,
        String name,
        GuideIcon icon,
        Kind kind,
        List<MetallurgyRecipe> recipes,
        List<FuelCombination> fuelCombinations
) {
    public MetallurgyCategory {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("invalid category id: " + id);
        }
        if (name == null || name.isBlank()) throw new IllegalArgumentException("category name cannot be blank");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(kind, "kind");
        recipes = List.copyOf(recipes);
        fuelCombinations = List.copyOf(fuelCombinations);
    }

    public enum Kind {
        METAL,
        FUEL,
        SLAG,
        INFO
    }
}
