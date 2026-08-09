package vn.haohan.metallurgy.guide;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import vn.haohan.metallurgy.machine.MachineType;
import vn.haohan.metallurgy.recipe.MetallurgyRecipe;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single read model for the guide. Recipe values come from RecipeLoader and
 * thermal/output values come from ConfigManager, so the renderer owns no game
 * balance data.
 */
public final class MetallurgyGuideCatalog {
    private static final List<CategoryDefinition> STANDARD_METALS = List.of(
            metal("copper", "Copper", GuideIcon.vanilla(Material.RAW_COPPER)),
            metal("iron", "Iron", GuideIcon.vanilla(Material.RAW_IRON)),
            metal("gold", "Gold", GuideIcon.vanilla(Material.RAW_GOLD)),
            metal("embersteel", "Embersteel", custom(CustomItem.EMBERSTEEL_INGOT)),
            metal("mithril", "Mithril", custom(CustomItem.MITHRIL_INGOT)),
            metal("soulsteel", "Soulsteel", custom(CustomItem.SOULSTEEL_INGOT)),
            metal("netherite", "Netherite", GuideIcon.vanilla(Material.NETHERITE_INGOT))
    );

    private final List<MetallurgyCategory> categories;
    private final OutputChance outputChance;
    private final List<RecipeNote> operatingNotes;

    private MetallurgyGuideCatalog(List<MetallurgyCategory> categories,
                                   OutputChance outputChance,
                                   List<RecipeNote> operatingNotes) {
        this.categories = List.copyOf(categories);
        this.outputChance = outputChance;
        this.operatingNotes = List.copyOf(operatingNotes);
    }

    public static MetallurgyGuideCatalog from(HaoHanMetallurgy plugin) {
        List<MetallurgyRecipe> recipes = new ArrayList<>(
                plugin.getRecipeLoader().getForMachine(MachineType.ANCIENT_FORGE));
        recipes.sort(Comparator.comparing(MetallurgyRecipe::getId));

        List<FuelCombination> fuels = readFuelCombinations(plugin);
        double cleanWithoutBorax = plugin.getConfigManager().isFailEnabled()
                ? plugin.getConfigManager().getCleanOutputChanceWithoutBorax() : 1.0;
        double cleanWithBorax = plugin.getConfigManager().isFailEnabled()
                ? plugin.getConfigManager().getCleanOutputChanceWithBorax() : 1.0;
        OutputChance output = new OutputChance(
                cleanWithoutBorax,
                cleanWithBorax,
                plugin.getConfigManager().getBoraxPerBatch());

        List<MetallurgyCategory> result = new ArrayList<>();
        for (CategoryDefinition definition : STANDARD_METALS) {
            List<MetallurgyRecipe> matching = recipes.stream()
                    .filter(recipe -> recipeKey(recipe).contains(definition.id()))
                    .toList();
            if (!matching.isEmpty()) {
                result.add(category(plugin, definition, matching, fuels));
            }
        }

        addDiscoveredCategories(plugin, result, recipes, fuels);
        result.add(new MetallurgyCategory(
                "fuel", categoryName(plugin, "fuel", "Fuel"), GuideIcon.vanilla(Material.COAL),
                MetallurgyCategory.Kind.FUEL, List.of(), fuels));

        List<MetallurgyRecipe> recycling = recipes.stream()
                .filter(recipe -> recipe.getId().contains("slag_recycling"))
                .toList();
        result.add(new MetallurgyCategory(
                "slag", categoryName(plugin, "slag", "Slag"), custom(CustomItem.IRON_SLAG),
                MetallurgyCategory.Kind.SLAG, recycling, fuels));
        result.add(new MetallurgyCategory(
                "info", categoryName(plugin, "info", "Info"), GuideIcon.vanilla(Material.KNOWLEDGE_BOOK),
                MetallurgyCategory.Kind.INFO, List.of(), List.of()));
        return new MetallurgyGuideCatalog(result, output, operatingNotes(plugin, null));
    }

    public List<MetallurgyCategory> categories() {
        return categories;
    }

    public OutputChance outputChance() {
        return outputChance;
    }

    public List<RecipeNote> operatingNotes() {
        return operatingNotes;
    }

    private static MetallurgyCategory category(HaoHanMetallurgy plugin,
                                                CategoryDefinition definition,
                                                List<MetallurgyRecipe> recipes,
                                                List<FuelCombination> fuels) {
        return new MetallurgyCategory(definition.id(),
                categoryName(plugin, definition.id(), definition.name()), definition.icon(),
                MetallurgyCategory.Kind.METAL, recipes, fuels);
    }

    private static List<FuelCombination> readFuelCombinations(HaoHanMetallurgy plugin) {
        List<FuelCombination> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : plugin.getConfigManager().getFuelCombinations().entrySet()) {
            String[] pair = entry.getKey().split("\\+", -1);
            if (pair.length != 2) continue;
            Material first = Material.matchMaterial(pair[0]);
            Material second = Material.matchMaterial(pair[1]);
            if (first == null || second == null || entry.getValue() < 0) continue;
            result.add(new FuelCombination(first, second, entry.getValue()));
        }
        result.sort(Comparator.comparingInt(FuelCombination::maxTemperature)
                .thenComparing(row -> row.first().name())
                .thenComparing(row -> row.second().name()));
        return List.copyOf(result);
    }

    private static void addDiscoveredCategories(HaoHanMetallurgy plugin,
                                                List<MetallurgyCategory> result,
                                                List<MetallurgyRecipe> recipes,
                                                List<FuelCombination> fuels) {
        Map<String, List<MetallurgyRecipe>> discovered = new LinkedHashMap<>();
        for (MetallurgyRecipe recipe : recipes) {
            if (STANDARD_METALS.stream().anyMatch(def -> recipeKey(recipe).contains(def.id()))) continue;
            String key = outputKey(recipe);
            discovered.computeIfAbsent(key, ignored -> new ArrayList<>()).add(recipe);
        }
        for (Map.Entry<String, List<MetallurgyRecipe>> entry : discovered.entrySet()) {
            MetallurgyRecipe first = entry.getValue().getFirst();
            result.add(new MetallurgyCategory(entry.getKey(),
                    categoryName(plugin, entry.getKey(), title(entry.getKey())), iconOf(first),
                    MetallurgyCategory.Kind.METAL, entry.getValue(), fuels));
        }
    }

    public static List<RecipeNote> operatingNotes(HaoHanMetallurgy plugin, MetallurgyRecipe recipe) {
        List<RecipeNote> notes = new ArrayList<>();
        var language = plugin.getLanguageManager();
        notes.add(new RecipeNote(language.plain("guide.notes.bellows", Map.of(
                "temperature", plugin.getConfigManager().getBellowsTemperatureBoost())),
                RecipeNote.Tone.INFO));
        notes.add(new RecipeNote(language.plain("guide.notes.piston", Map.of(
                "temperature", plugin.getConfigManager().getPistonBellowsTemperatureBoost(),
                "ticks", plugin.getConfigManager().getPistonBellowsProgressTicks())),
                RecipeNote.Tone.GOOD));
        notes.add(new RecipeNote(language.plain("guide.notes.below-minimum"), RecipeNote.Tone.WARNING));
        notes.add(new RecipeNote(language.plain("guide.notes.above-maximum"), RecipeNote.Tone.DANGER));
        notes.add(new RecipeNote(language.plain("guide.notes.correct-flux"), RecipeNote.Tone.INFO));
        notes.add(new RecipeNote(language.plain("guide.notes.consumed-flux"), RecipeNote.Tone.WARNING));
        if (recipe != null && recipe.requiresColdQuench()) {
            notes.add(new RecipeNote(language.plain("guide.notes.cold-required"), RecipeNote.Tone.INFO));
        }
        if (recipe != null && recipe.requiresSoulFire()) {
            notes.add(new RecipeNote(language.plain("guide.notes.soul-required"), RecipeNote.Tone.INFO));
        }
        return List.copyOf(notes);
    }

    private static GuideIcon iconOf(MetallurgyRecipe recipe) {
        String customId = recipe.getOutput().customItemId();
        if (customId != null) return new GuideIcon(recipe.getOutput().material(), customId);
        return GuideIcon.vanilla(recipe.getOutput().material());
    }

    private static String recipeKey(MetallurgyRecipe recipe) {
        return (recipe.getId() + " " + outputKey(recipe)).toLowerCase(Locale.ROOT);
    }

    private static String outputKey(MetallurgyRecipe recipe) {
        String raw = recipe.getOutput().customItemId();
        if (raw == null && recipe.getOutput().material() != null) {
            raw = recipe.getOutput().material().name().toLowerCase(Locale.ROOT);
        }
        if (raw == null || raw.isBlank()) return "other";
        return raw.replace("_ingot", "").replace("_scrap", "");
    }

    private static String title(String id) {
        StringBuilder result = new StringBuilder();
        for (String word : id.split("_")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static String categoryName(HaoHanMetallurgy plugin, String id, String fallback) {
        return plugin.getLanguageManager().plainOr("guide.category." + id, fallback);
    }

    private static CategoryDefinition metal(String id, String name, GuideIcon icon) {
        return new CategoryDefinition(id, name, icon);
    }

    private static GuideIcon custom(CustomItem item) {
        return GuideIcon.custom(item.getMaterial(), item.getId());
    }

    private record CategoryDefinition(String id, String name, GuideIcon icon) {}
}
