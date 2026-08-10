package vn.haohan.metallurgy.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vn.haohan.metallurgy.machine.MachineType;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho MetallurgyRecipe — không cần Bukkit server.
 */
class MetallurgyRecipeTest {

    private JsonObject readBundledRecipe(String name) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("recipes/" + name)) {
            assertNotNull(stream, "Missing bundled recipe " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private MetallurgyRecipe buildTestRecipe() {
        var inputs = List.of(
            new MetallurgyRecipe.Ingredient(Material.IRON_INGOT, 4),
            new MetallurgyRecipe.Ingredient(Material.BLAZE_POWDER, 2)
        );
        var output = new MetallurgyRecipe.OutputItem(
            Material.IRON_INGOT, 2,
            "&6Embersteel Ingot",
            List.of("&7Test lore"),
            1001,
            "embersteel_ingot"
        );
        return new MetallurgyRecipe(
            "ancient_forge/embersteel_ingot",
            MachineType.ANCIENT_FORGE.name(),
            inputs,
            output,
            400,
            30,
            800,
            1200,
            "haohan:metallurgy/root"
        );
    }

    @Test
    void testRecipeIdIsSet() {
        var recipe = buildTestRecipe();
        assertEquals("ancient_forge/embersteel_ingot", recipe.getId());
    }

    @Test
    void testMachineTypeIsCorrect() {
        var recipe = buildTestRecipe();
        assertEquals("ANCIENT_FORGE", recipe.getMachineType());
    }

    @Test
    void testInputsAreImmutable() {
        var recipe = buildTestRecipe();
        assertThrows(UnsupportedOperationException.class, () -> {
            recipe.getInputs().add(new MetallurgyRecipe.Ingredient(Material.COAL, 1));
        });
    }

    @Test
    void testIngredientMatches() {
        var ingredient = new MetallurgyRecipe.Ingredient(Material.IRON_INGOT, 4);
        assertTrue(ingredient.matches(Material.IRON_INGOT, 4));
        assertTrue(ingredient.matches(Material.IRON_INGOT, 5)); // có nhiều hơn → ok
        assertFalse(ingredient.matches(Material.IRON_INGOT, 3)); // thiếu
        assertFalse(ingredient.matches(Material.GOLD_INGOT, 4)); // sai material
    }

    @Test
    void testFuelAndTime() {
        var recipe = buildTestRecipe();
        assertEquals(400, recipe.getFuelCost());
        assertEquals(30, recipe.getTimeSeconds());
        assertEquals(800, recipe.getMinTemperature());
        assertEquals(1200, recipe.getMaxTemperature());
        assertEquals("haohan:metallurgy/root", recipe.getRequiredAdvancement());
    }

    @Test
    void testOutputFields() {
        var recipe = buildTestRecipe();
        var output = recipe.getOutput();
        assertEquals(Material.IRON_INGOT, output.material());
        assertEquals(2, output.amount());
        assertEquals(1001, output.customModelData());
        assertEquals(1, output.lore().size());
        assertEquals("embersteel_ingot", output.customItemId());
    }

    @Test
    void testRequiredAdditive() {
        var inputs = List.of(new MetallurgyRecipe.Ingredient(Material.IRON_INGOT, 4));
        var output = new MetallurgyRecipe.OutputItem(Material.IRON_INGOT, 2, "Name", List.of(), 0, null);
        var recipe = new MetallurgyRecipe(
            "test_additive",
            "ANCIENT_FORGE",
            inputs,
            output,
            100,
            10,
            500,
            800,
            null,
            0.1,
            Material.SOUL_SOIL
        );
        assertEquals(Material.SOUL_SOIL, recipe.getRequiredAdditive());
    }

    @Test
    void temperatureBelowPurificationIncreasesSlagChance() {
        var recipe = new MetallurgyRecipe(
            "temperature_quality",
            "ANCIENT_FORGE",
            List.of(new MetallurgyRecipe.Ingredient(Material.RAW_IRON, 1)),
            new MetallurgyRecipe.OutputItem(Material.IRON_INGOT, 1, null, List.of(), 0, null),
            100,
            10,
            1000,
            1500,
            1750,
            null,
            0.10,
            0.90,
            List.of(),
            1,
            -1.0,
            false,
            false
        );

        assertEquals(0.90, recipe.getTemperatureFailChance(1000), 0.0001);
        assertEquals(0.50, recipe.getTemperatureFailChance(1250), 0.0001);
        assertEquals(0.10, recipe.getTemperatureFailChance(1500), 0.0001);
        assertEquals(0.10, recipe.getTemperatureFailChance(1700), 0.0001);
        assertEquals(0.80, recipe.getThermalFailurePenalty(1000), 0.0001);
        assertEquals(0.40, recipe.getThermalFailurePenalty(1250), 0.0001);
        assertEquals(0.0, recipe.getThermalFailurePenalty(1500), 0.0001);
    }

    @Test
    void sharedSlagResolverUsesTheRecipeMetal() {
        MetallurgyRecipe recipe = new MetallurgyRecipe(
                "ancient_forge/copper_ingot", "ANCIENT_FORGE",
                List.of(new MetallurgyRecipe.Ingredient(Material.RAW_COPPER, 1)),
                new MetallurgyRecipe.OutputItem(Material.COPPER_INGOT, 1,
                        null, List.of(), 0, null),
                100, 10, 700, 800, 950, null,
                0.08, 0.75, List.of(), 1, -1.0, false, false);

        assertEquals(CustomItem.COPPER_SLAG, MetallurgyOutputs.slagFor(recipe).orElseThrow());
    }

    @Test
    void recipeAcceptsAlternativeSpecialAdditives() {
        var recipe = new MetallurgyRecipe(
            "mithril_flux",
            "ANCIENT_FORGE",
            List.of(new MetallurgyRecipe.Ingredient(Material.PRISMARINE_SHARD, 4)),
            new MetallurgyRecipe.OutputItem(Material.IRON_INGOT, 1, null, List.of(), 0, "mithril_ingot"),
            300,
            15,
            1300,
            1600,
            1800,
            null,
            0.10,
            0.90,
            List.of(Material.QUARTZ, Material.GLOWSTONE_DUST),
            2,
            0.25,
            true,
            false
        );

        assertTrue(recipe.acceptsAdditive(Material.QUARTZ));
        assertTrue(recipe.acceptsAdditive(Material.GLOWSTONE_DUST));
        assertFalse(recipe.acceptsAdditive(Material.REDSTONE));
        assertEquals(2, recipe.getAdditiveAmount());
        assertEquals(0.25, recipe.getAdditiveCleanOutputBonus(), 0.0001);
        assertTrue(recipe.requiresColdQuench());
    }

    @Test
    void ingredientAcceptsSoulSandOrSoulSoil() {
        var ingredient = new MetallurgyRecipe.Ingredient(
                Material.SOUL_SOIL, 1, null, List.of(Material.SOUL_SAND));

        assertTrue(ingredient.matches(Material.SOUL_SOIL, 1));
        assertTrue(ingredient.matches(Material.SOUL_SAND, 1));
        assertFalse(ingredient.matches(Material.SOUL_FIRE, 1));
    }

    @Test
    void bundledAlloyRecipesUseRequestedInputsAndBoraxSchema() throws Exception {
        JsonObject ember = readBundledRecipe("example_forge.json");
        assertEquals("IRON_INGOT", ember.getAsJsonArray("inputs").get(0).getAsJsonObject().get("material").getAsString());
        assertEquals("GOLD_INGOT", ember.getAsJsonArray("inputs").get(1).getAsJsonObject().get("material").getAsString());

        JsonObject mithril = readBundledRecipe("mithril_ingot_smelting.json");
        assertEquals("embersteel_ingot", mithril.getAsJsonArray("inputs").get(0).getAsJsonObject().get("custom_item").getAsString());
        assertEquals("DIAMOND", mithril.getAsJsonArray("inputs").get(1).getAsJsonObject().get("material").getAsString());

        JsonObject soulsteel = readBundledRecipe("soulsteel_ingot_smelting.json");
        var soulChoices = soulsteel.getAsJsonArray("inputs").get(1).getAsJsonObject().getAsJsonArray("materials");
        assertEquals("SOUL_SOIL", soulChoices.get(0).getAsString());
        assertEquals("SOUL_SAND", soulChoices.get(1).getAsString());

        JsonObject netherite = readBundledRecipe("netherite_ingot_smelting.json");
        assertEquals("NETHERITE_SCRAP", netherite.getAsJsonArray("inputs").get(0).getAsJsonObject().get("material").getAsString());
        assertEquals(4, netherite.getAsJsonArray("inputs").get(0).getAsJsonObject().get("amount").getAsInt());
        assertEquals("GOLD_INGOT", netherite.getAsJsonArray("inputs").get(1).getAsJsonObject().get("material").getAsString());
        assertEquals(4, netherite.getAsJsonArray("inputs").get(1).getAsJsonObject().get("amount").getAsInt());

        List<String> ingotRecipes = List.of(
                "example_forge.json", "raw_iron_smelting.json", "raw_copper_smelting.json",
                "raw_gold_smelting.json", "netherite_ingot_smelting.json",
                "mithril_ingot_smelting.json", "soulsteel_ingot_smelting.json");
        for (String name : ingotRecipes) {
            JsonObject recipe = readBundledRecipe(name);
            assertEquals(6, recipe.get("_schema_version").getAsInt());
            var flux = recipe.getAsJsonArray("fluxes").get(0).getAsJsonObject();
            assertEquals("borax_powder", flux.get("custom_item").getAsString());
            assertEquals(1, flux.get("amount").getAsInt());
        }

        JsonObject copperSlag = readBundledRecipe("copper_slag_recycling.json");
        assertEquals("SAND", copperSlag.getAsJsonArray("fluxes").get(0)
                .getAsJsonObject().get("material").getAsString());
        assertEquals("RED_SAND", copperSlag.getAsJsonArray("fluxes").get(1)
                .getAsJsonObject().get("material").getAsString());

        JsonObject ironSlag = readBundledRecipe("iron_slag_recycling.json");
        assertEquals("FLINT", ironSlag.getAsJsonArray("fluxes").get(0)
                .getAsJsonObject().get("material").getAsString());
        assertEquals("QUARTZ", ironSlag.getAsJsonArray("fluxes").get(1)
                .getAsJsonObject().get("material").getAsString());

        JsonObject mithrilSlag = readBundledRecipe("mithril_slag_recycling.json");
        assertEquals(2, mithrilSlag.getAsJsonArray("fluxes").get(0)
                .getAsJsonObject().get("amount").getAsInt());
        assertEquals("GLOWSTONE_DUST", mithrilSlag.getAsJsonArray("fluxes").get(1)
                .getAsJsonObject().get("material").getAsString());

        JsonObject netheriteSlag = readBundledRecipe("netherite_slag_recycling.json");
        assertEquals("BLAZE_POWDER", netheriteSlag.getAsJsonArray("fluxes").get(0)
                .getAsJsonObject().get("material").getAsString());
        assertEquals(0.20, netheriteSlag.getAsJsonArray("fluxes").get(0)
                .getAsJsonObject().get("clean_output_bonus").getAsDouble(), 0.0001);
    }

    @Test
    void bundledRecipesHaveConsistentAmountsTemperaturesAndReachableHeat() throws Exception {
        List<String> names = List.of(
                "example_forge.json", "raw_iron_smelting.json", "raw_copper_smelting.json",
                "raw_gold_smelting.json", "netherite_ingot_smelting.json",
                "mithril_ingot_smelting.json", "soulsteel_ingot_smelting.json",
                "copper_slag_recycling.json", "iron_slag_recycling.json",
                "gold_slag_recycling.json", "netherite_slag_recycling.json",
                "mithril_slag_recycling.json");
        var ids = new HashSet<String>();

        try (var stream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(stream);
            var config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            var combinations = config.getConfigurationSection("temperature.fuel-combinations");
            assertNotNull(combinations);
            int hottestFuelPair = combinations.getKeys(false).stream()
                    .mapToInt(combinations::getInt).max().orElse(0);

            for (String name : names) {
                JsonObject recipe = readBundledRecipe(name);
                assertTrue(ids.add(recipe.get("id").getAsString()), "Duplicate ID in " + name);
                assertTrue(recipe.getAsJsonArray("inputs").size() >= 1
                        && recipe.getAsJsonArray("inputs").size() <= 2, name);
                recipe.getAsJsonArray("inputs").forEach(input ->
                        assertTrue(input.getAsJsonObject().get("amount").getAsInt() > 0, name));
                assertTrue(recipe.getAsJsonObject("output").get("amount").getAsInt() > 0, name);
                assertTrue(recipe.get("time_seconds").getAsInt() > 0, name);
                assertTrue(recipe.get("fuel_cost").getAsInt() >= 0, name);

                int min = recipe.get("min_temperature").getAsInt();
                int refine = recipe.get("purification_temperature").getAsInt();
                int max = recipe.get("max_temperature").getAsInt();
                assertTrue(min >= 0 && min <= refine && refine <= max, name);
                assertTrue(max > min, name);
                assertTrue(hottestFuelPair >= min, "No configured fuel pair can start " + name);
                assertTrue(recipe.get("fail_chance").getAsDouble() >= 0.0
                        && recipe.get("fail_chance").getAsDouble() <= 1.0, name);
                assertTrue(recipe.get("underheat_fail_chance").getAsDouble() >= 0.0
                        && recipe.get("underheat_fail_chance").getAsDouble() <= 1.0, name);
            }
        }
    }

    @Test
    void bundledConfigContainsTieredFuelProfiles() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(stream);
            var config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertEquals(11, config.getInt("config-version"));
            assertEquals(0.8, config.getDouble("machines.time-speed-multiplier"), 0.0001);
            assertEquals(3, config.getInt("progression.mining-requirements.minecraft:coal_ore"));
            assertEquals(3, config.getInt("progression.mining-requirements.minecraft:deepslate_coal_ore"));
            assertEquals(0.18, config.getDouble("jute.drops.SHORT_GRASS.chance"), 0.0001);
            assertEquals(0.20, config.getDouble("jute.drops.FERN.chance"), 0.0001);
            assertEquals(0.30, config.getDouble("jute.drops.TALL_GRASS.chance"), 0.0001);
            assertEquals(2, config.getInt("jute.drops.TALL_GRASS.max-amount"));
            assertEquals(0.32, config.getDouble("jute.drops.LARGE_FERN.chance"), 0.0001);
            assertEquals(2, config.getInt("jute.drops.LARGE_FERN.max-amount"));
            assertEquals(4.0, config.getDouble("charcoal-kiln.ignition-seconds"), 0.0001);
            assertEquals(5, config.getInt("progression.custom-item-stats.bow_drill.max-damage"));
            assertEquals(120, config.getInt("charcoal-kiln.duration-seconds"));
            assertEquals(150, config.getInt("fuel-groups.fresh-plant.temperature-limit"));
            assertEquals(200, config.getInt("fuel-groups.wool.temperature-limit"));
            assertEquals(400, config.getInt("fuel-groups.wood.temperature-limit"));
            assertEquals(800, config.getInt("fuel-groups.carbon.temperature-limit"));
            assertEquals(1100, config.getInt("fuel-groups.lava.temperature-limit"));
            assertEquals(1600, config.getInt("temperature.fuel-limits.BLAZE_ROD"));
            assertEquals(800, config.getInt("temperature.ignition-boosts.BLAZE_POWDER"));
            assertEquals(2050, config.getInt("temperature.fuel-combinations.BLAZE_ROD+BLAZE_ROD"));
            assertEquals(40, config.getInt("temperature.bellows-boost"));
            assertEquals(30, config.getInt("temperature.piston-bellows-boost"));
            assertEquals(5, config.getInt("temperature.piston-progress-ticks"));
            assertEquals(1, config.getInt("additives.borax-per-batch"));
            assertEquals(0.95, config.getDouble("additives.clean-output-chance-with-borax"), 0.0001);
            assertEquals(0.08, config.getDouble("additives.clean-output-chance-without-borax"), 0.0001);
            assertEquals(0.10, config.getDouble("vanilla-furnaces.clean-output-chance"), 0.0001);
            assertTrue(config.getBoolean("fail.enabled"));
        }
    }
}
