package vn.haohan.metallurgy.recipe;

import com.google.gson.*;
import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.machine.MachineType;
import org.bukkit.Material;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Đọc và parse tất cả file *.json trong thư mục recipes/.
 * Lưu vào Map theo MachineType để tra cứu nhanh.
 */
public class RecipeLoader {

    private final HaoHanMetallurgy plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** id → recipe */
    private final Map<String, MetallurgyRecipe> recipesById = new LinkedHashMap<>();

    /** machineType → list of recipes */
    private final Map<MachineType, List<MetallurgyRecipe>> recipesByMachine = new EnumMap<>(MachineType.class);

    public RecipeLoader(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
    }

    // ── Public API ─────────────────────────────────────────────

    /** Load (hoặc reload) tất cả recipes từ disk. */
    public void loadAll() {
        recipesById.clear();
        recipesByMachine.clear();

        File recipeDir = new File(plugin.getDataFolder(), "recipes");
        if (!recipeDir.exists() && !recipeDir.mkdirs()) {
            plugin.getPluginLogger().error("Cannot create recipes directory: " + recipeDir.getPath());
            return;
        }

        for (String fileName : List.of(
                "example_forge.json",
                "raw_iron_smelting.json",
                "raw_copper_smelting.json",
                "raw_gold_smelting.json",
                "netherite_ingot_smelting.json",
                "mithril_ingot_smelting.json",
                "soulsteel_ingot_smelting.json",
                "copper_slag_recycling.json",
                "iron_slag_recycling.json",
                "gold_slag_recycling.json",
                "netherite_slag_recycling.json",
                "mithril_slag_recycling.json")) {
            File target = new File(recipeDir, fileName);
            if (!target.exists()) {
                plugin.saveResource("recipes/" + fileName, false);
            } else if (needsBundledRecipeMigration(target)) {
                try {
                    Path backup = target.toPath().resolveSibling(fileName + ".before-v6.bak");
                    if (!Files.exists(backup)) {
                        Files.copy(target.toPath(), backup);
                    }
                    plugin.saveResource("recipes/" + fileName, true);
                    plugin.getPluginLogger().info("Migrated bundled recipe to per-recipe flux schema: "
                            + fileName + " (backup: " + backup.getFileName() + ")");
                } catch (IOException e) {
                    plugin.getPluginLogger().error("Could not migrate bundled recipe: " + fileName, e);
                }
            }
        }

        File[] files;
        try {
            files = recipeDir.listFiles((dir, name) -> name.endsWith(".json"));
        } catch (SecurityException e) {
            plugin.getPluginLogger().error("Cannot read recipes directory: " + recipeDir.getPath(), e);
            return;
        }

        if (files == null || files.length == 0) {
            plugin.getPluginLogger().warn("No recipe files found in " + recipeDir.getPath());
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));
        int loaded = 0;
        for (File file : files) {
            try {
                MetallurgyRecipe recipe = parseFile(file);
                if (recipe != null && register(recipe)) loaded++;
            } catch (Exception e) {
                plugin.getPluginLogger().error("Failed to load recipe file: " + file.getName(), e);
            }
        }

        plugin.getPluginLogger().info("Loaded " + loaded + " recipe(s) from " + files.length + " file(s).");
    }

    /** Lấy recipe theo ID. */
    public Optional<MetallurgyRecipe> getById(String id) {
        return Optional.ofNullable(recipesById.get(id));
    }

    /** Lấy tất cả recipe của một loại máy. */
    public List<MetallurgyRecipe> getForMachine(MachineType type) {
        return List.copyOf(recipesByMachine.getOrDefault(type, Collections.emptyList()));
    }

    /** Tổng số recipe đã load. */
    public int count() {
        return recipesById.size();
    }

    /** Snapshot of every loaded machine recipe for integrations and guides. */
    public List<MetallurgyRecipe> all() {
        return List.copyOf(recipesById.values());
    }

    // ── Internal ───────────────────────────────────────────────

    private MetallurgyRecipe parseFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        JsonObject obj = JsonParser.parseString(content).getAsJsonObject();

        String id = obj.get("id").getAsString();
        String machineTypeStr = obj.get("machine_type").getAsString();
        if (id.isBlank() || !id.matches("[a-z0-9_./-]+")) {
            plugin.getPluginLogger().error("Invalid recipe ID in " + file.getName() + ": " + id);
            return null;
        }
        try {
            MachineType.valueOf(machineTypeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getPluginLogger().error("Unknown machine type '" + machineTypeStr
                    + "' in " + file.getName());
            return null;
        }

        // Parse inputs
        List<MetallurgyRecipe.Ingredient> inputs = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray("inputs")) {
            JsonObject inp = el.getAsJsonObject();
            String customItem = inp.has("custom_item") ? inp.get("custom_item").getAsString() : null;
            Material mat = null;
            List<Material> alternativeMaterials = new ArrayList<>();
            if (customItem != null) {
                java.util.Optional<vn.haohan.metallurgy.item.CustomItem> ciOpt = vn.haohan.metallurgy.item.CustomItem.getById(customItem);
                if (ciOpt.isPresent()) {
                    mat = ciOpt.get().getMaterial();
                } else {
                    plugin.getPluginLogger().error("Unknown custom item ID '" + customItem + "' in inputs in " + file.getName());
                    return null;
                }
            } else if (inp.has("materials")) {
                for (JsonElement materialElement : inp.getAsJsonArray("materials")) {
                    Material candidate = requireMaterial(materialElement.getAsString(), file);
                    if (candidate == null) return null;
                    if (mat == null) {
                        mat = candidate;
                    } else if (!alternativeMaterials.contains(candidate)) {
                        alternativeMaterials.add(candidate);
                    }
                }
                if (mat == null) {
                    plugin.getPluginLogger().error("Empty materials list in inputs in " + file.getName());
                    return null;
                }
            } else {
                mat = requireMaterial(inp.get("material").getAsString(), file);
                if (mat == null) return null;
            }
            int amount = inp.get("amount").getAsInt();
            if (amount <= 0) {
                plugin.getPluginLogger().error("Input amount must be positive in " + file.getName());
                return null;
            }
            inputs.add(new MetallurgyRecipe.Ingredient(mat, amount, customItem, alternativeMaterials));
        }
        if (inputs.isEmpty() || inputs.size() > 2) {
            plugin.getPluginLogger().error("Ancient Forge recipes require one or two inputs in "
                    + file.getName());
            return null;
        }

        // Parse output
        JsonObject outObj = obj.getAsJsonObject("output");
        String customItemId = outObj.has("custom_item") ? outObj.get("custom_item").getAsString() : null;
        Material outMat = null;
        
        if (customItemId != null) {
            java.util.Optional<vn.haohan.metallurgy.item.CustomItem> ciOpt = vn.haohan.metallurgy.item.CustomItem.getById(customItemId);
            if (ciOpt.isPresent()) {
                outMat = ciOpt.get().getMaterial();
            } else {
                plugin.getPluginLogger().error("Unknown custom item ID '" + customItemId + "' in " + file.getName());
                return null;
            }
        } else {
            outMat = requireMaterial(outObj.get("material").getAsString(), file);
            if (outMat == null) return null;
        }

        String displayName = outObj.has("display_name") ? outObj.get("display_name").getAsString() : null;
        List<String> lore = new ArrayList<>();
        if (outObj.has("lore")) {
            outObj.getAsJsonArray("lore").forEach(l -> lore.add(l.getAsString()));
        }
        int cmd = outObj.has("custom_model_data") ? outObj.get("custom_model_data").getAsInt() : 0;

        int outputAmount = outObj.get("amount").getAsInt();
        if (outputAmount <= 0) {
            plugin.getPluginLogger().error("Output amount must be positive in " + file.getName());
            return null;
        }
        MetallurgyRecipe.OutputItem output = new MetallurgyRecipe.OutputItem(
                outMat,
                outputAmount,
                displayName,
                lore,
                cmd,
                customItemId
        );

        int fuelCost     = obj.has("fuel_cost")       ? obj.get("fuel_cost").getAsInt()       : 0;
        int timeSeconds  = obj.has("time_seconds")    ? obj.get("time_seconds").getAsInt()    : 10;
        int minTemp      = obj.has("min_temperature") ? obj.get("min_temperature").getAsInt() : 0;
        int purificationTemp = obj.has("purification_temperature")
                ? obj.get("purification_temperature").getAsInt()
                : minTemp;
        int maxTemp      = obj.has("max_temperature") ? obj.get("max_temperature").getAsInt() : (minTemp + 400);
        String reqAdv    = obj.has("required_advancement") ? obj.get("required_advancement").getAsString() : null;

        double failChance = 0.0;
        if (obj.has("fail_chance")) {
            failChance = obj.get("fail_chance").getAsDouble();
        } else {
            // Tự động tính toán theo quy tắc vật chất
            Material m = output.material();
            String idStr = id.toLowerCase();
            if (m != null) {
                String matName = m.name();
                if (matName.contains("GOLD")) {
                    failChance = 0.0;
                } else if (matName.contains("IRON") || matName.contains("COPPER") || matName.contains("NETHERITE") || idStr.contains("steel")) {
                    failChance = plugin.getConfigManager().getFailBaseChance();
                }
            }
        }

        double underheatFailChance = obj.has("underheat_fail_chance")
                ? obj.get("underheat_fail_chance").getAsDouble()
                : failChance;

        List<Material> requiredAdditives = new ArrayList<>();
        if (obj.has("additive")) {
            String addStr = obj.get("additive").getAsString();
            Material additive = requireMaterial(addStr, file);
            if (additive == null) return null;
            requiredAdditives.add(additive);
        }
        if (obj.has("additives")) {
            for (JsonElement element : obj.getAsJsonArray("additives")) {
                Material additive = requireMaterial(element.getAsString(), file);
                if (additive == null) return null;
                if (!requiredAdditives.contains(additive)) requiredAdditives.add(additive);
            }
        }
        int additiveAmount = obj.has("additive_amount") ? obj.get("additive_amount").getAsInt() : 1;
        double additiveCleanOutputBonus = obj.has("additive_clean_output_bonus")
                ? obj.get("additive_clean_output_bonus").getAsDouble()
                : -1.0;
        List<MetallurgyRecipe.Flux> fluxes = new ArrayList<>();
        if (obj.has("fluxes")) {
            for (JsonElement element : obj.getAsJsonArray("fluxes")) {
                JsonObject fluxObject = element.getAsJsonObject();
                String customFlux = fluxObject.has("custom_item")
                        ? fluxObject.get("custom_item").getAsString() : null;
                Material fluxMaterial;
                if (customFlux != null) {
                    var custom = vn.haohan.metallurgy.item.CustomItem.getById(customFlux);
                    if (custom.isEmpty()) {
                        plugin.getPluginLogger().error("Unknown custom flux '" + customFlux
                                + "' in " + file.getName());
                        return null;
                    }
                    fluxMaterial = custom.get().getMaterial();
                } else if (fluxObject.has("material")) {
                    fluxMaterial = requireMaterial(fluxObject.get("material").getAsString(), file);
                    if (fluxMaterial == null) return null;
                } else {
                    plugin.getPluginLogger().error("Flux needs material or custom_item in "
                            + file.getName());
                    return null;
                }
                int amount = fluxObject.has("amount") ? fluxObject.get("amount").getAsInt() : 1;
                double bonus = fluxObject.has("clean_output_bonus")
                        ? fluxObject.get("clean_output_bonus").getAsDouble() : -1.0;
                if (amount <= 0 || bonus < -1.0 || bonus > 1.0) {
                    plugin.getPluginLogger().error("Invalid flux amount/bonus in " + file.getName());
                    return null;
                }
                fluxes.add(new MetallurgyRecipe.Flux(
                        fluxMaterial, customFlux, amount, bonus));
            }
        } else {
            for (Material material : requiredAdditives) {
                fluxes.add(new MetallurgyRecipe.Flux(material, null, additiveAmount,
                        additiveCleanOutputBonus));
            }
        }
        boolean requiresColdQuench = obj.has("requires_cold_quench")
                && obj.get("requires_cold_quench").getAsBoolean();
        boolean requiresSoulFire = obj.has("requires_soul_fire")
                && obj.get("requires_soul_fire").getAsBoolean();

        if (fuelCost < 0 || timeSeconds <= 0 || minTemp < 0 || maxTemp <= minTemp) {
            plugin.getPluginLogger().error("Invalid fuel/time/temperature values in " + file.getName());
            return null;
        }
        if (failChance < 0.0 || failChance > 1.0
                || underheatFailChance < 0.0 || underheatFailChance > 1.0) {
            plugin.getPluginLogger().error("Recipe chances must be between 0 and 1 in " + file.getName());
            return null;
        }
        if (additiveCleanOutputBonus > 1.0) {
            plugin.getPluginLogger().error("Additive clean-output bonus must not exceed 1 in "
                    + file.getName());
            return null;
        }
        if (additiveAmount <= 0) {
            plugin.getPluginLogger().error("Additive amount must be positive in " + file.getName());
            return null;
        }
        if (purificationTemp < minTemp || purificationTemp > maxTemp) {
            plugin.getPluginLogger().error("Invalid purification temperature in " + file.getName()
                    + ": expected " + minTemp + " <= purification_temperature <= " + maxTemp);
            return null;
        }

        return new MetallurgyRecipe(id, machineTypeStr, inputs, output, fuelCost, timeSeconds,
                minTemp, purificationTemp, maxTemp, reqAdv, failChance, underheatFailChance,
                requiredAdditives, additiveAmount, additiveCleanOutputBonus,
                requiresColdQuench, requiresSoulFire, fluxes);
    }

    private Material requireMaterial(String name, File file) {
        Material mat = Material.matchMaterial(name);
        if (mat == null) {
            plugin.getPluginLogger().error("Unknown material '" + name + "' in " + file.getName());
        }
        return mat;
    }

    private boolean needsBundledRecipeMigration(File file) {
        try {
            JsonObject object = JsonParser.parseString(
                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            return !object.has("_schema_version") || object.get("_schema_version").getAsInt() < 6;
        } catch (Exception e) {
            plugin.getPluginLogger().warn("Skipping migration check for invalid recipe " + file.getName()
                    + ": " + e.getMessage());
            return false;
        }
    }

    private boolean register(MetallurgyRecipe recipe) {
        if (recipesById.containsKey(recipe.getId())) {
            plugin.getPluginLogger().error("Duplicate recipe ID '" + recipe.getId() + "' — skipping.");
            return false;
        }
        recipesById.put(recipe.getId(), recipe);

        MachineType type;
        try {
            type = MachineType.valueOf(recipe.getMachineType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getPluginLogger().warn("Unknown machine type '" + recipe.getMachineType()
                    + "' in recipe '" + recipe.getId() + "' — skipping.");
            recipesById.remove(recipe.getId());
            return false;
        }

        recipesByMachine.computeIfAbsent(type, k -> new ArrayList<>()).add(recipe);
        plugin.getPluginLogger().debug("Registered recipe: " + recipe.getId());
        return true;
    }
}
