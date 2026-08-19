package vn.haohan.metallurgy.recipe;

import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.ItemResult;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeType;
import vn.haohan.itemcore.api.recipe.ShapedRecipeDefinition;
import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bridges vanilla datapack recipes with plugin-owned custom items.
 *
 * The datapack recipes stay registered so the vanilla client can calculate
 * Recipe Book availability and place ingredients. This listener then validates
 * the PDC-backed custom ingredients and replaces the preview/output with the
 * canonical ItemStack created by ItemManager.
 */
public class CraftingRecipeManager implements Listener {

    private static final Map<String, CustomItem> PICKAXE_INGREDIENTS = Map.ofEntries(
            Map.entry("copper_slag_pickaxe", CustomItem.COPPER_SLAG),
            Map.entry("iron_slag_pickaxe", CustomItem.IRON_SLAG),
            Map.entry("gold_slag_pickaxe", CustomItem.GOLD_SLAG),
            Map.entry("embersteel_slag_pickaxe", CustomItem.EMBERSTEEL_SLAG),
            Map.entry("soulsteel_slag_pickaxe", CustomItem.SOULSTEEL_SLAG),
            Map.entry("netherite_slag_pickaxe", CustomItem.NETHERITE_SLAG),
            Map.entry("mithril_slag_pickaxe", CustomItem.MITHRIL_SLAG),
            Map.entry("embersteel_pickaxe", CustomItem.EMBERSTEEL_INGOT),
            Map.entry("mithril_pickaxe", CustomItem.MITHRIL_INGOT),
            Map.entry("soulsteel_pickaxe", CustomItem.SOULSTEEL_INGOT));

    private final HaoHanMetallurgy plugin;
    private final Set<NamespacedKey> recipeKeys = new LinkedHashSet<>();
    private final Set<String> browserRecipeIds = new LinkedHashSet<>();

    public CraftingRecipeManager(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        recipeKeys.clear();
        unregisterBrowserRecipes();
        removeVanillaNetheriteRecipes();
        registerMithrilCompression();
        registerBoraxGrinding();
        registerBowDrill();
        registerCharcoalBlockPacking();
        registerCharcoalBlockUnpacking();
        registerAlloyPickaxes();
        registerSlagPickaxes();
        registerBrowserRecipes();
        discoverRecipesForOnlinePlayers();
    }

    /**
     * Own the Netherite lock in the plugin so the companion datapack is not
     * needed to disable vanilla crafting/smithing paths.
     */
    private void removeVanillaNetheriteRecipes() {
        List<NamespacedKey> keys = new java.util.ArrayList<>();
        java.util.Iterator<Recipe> recipes = plugin.getServer().recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (!(recipe instanceof Keyed keyed)
                    || !NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace())) {
                continue;
            }
            String id = keyed.getKey().getKey();
            if ("netherite_ingot".equals(id)
                    || "netherite_scrap".equals(id)
                    || "netherite_scrap_from_blasting".equals(id)
                    || (id.startsWith("netherite_") && id.endsWith("_smithing"))) {
                keys.add(keyed.getKey());
            }
        }
        keys.forEach(plugin.getServer()::removeRecipe);
    }

    /**
     * Publishes the crafting recipes to ItemCore's browser catalog.  Bukkit's
     * recipe book and ItemCore's recipe viewer are separate registries, so
     * registering only a Bukkit recipe is not enough for right-click lookup.
     */
    private void registerBrowserRecipes() {
        registerBrowser(new RecipeDefinition("haohanmetallurgy:mithril_ingot_to_shards",
                RecipeType.SHAPELESS,
                java.util.List.of(item(CustomItem.MITHRIL_INGOT)),
                result(CustomItem.MITHRIL_SHARD, 9)));
        registerBrowser(new ShapedRecipeDefinition("haohanmetallurgy:mithril_shards_to_ingot",
                java.util.List.of("SSS", "SSS", "SSS"),
                java.util.Map.of('S', item(CustomItem.MITHRIL_SHARD)),
                result(CustomItem.MITHRIL_INGOT, 1)));
        registerBrowser(new RecipeDefinition("haohanmetallurgy:raw_borax_to_powder",
                RecipeType.SHAPELESS,
                java.util.List.of(item(CustomItem.RAW_BORAX)),
                result(CustomItem.BORAX_POWDER, 1)));
        registerBrowser(new RecipeDefinition("haohanmetallurgy:bow_drill",
                RecipeType.SHAPELESS,
                java.util.List.of(material(Material.STICK, 2), item(CustomItem.JUTE_CORD)),
                result(CustomItem.BOW_DRILL, 1)));
        registerBrowser(new RecipeDefinition("haohanmetallurgy:charcoal_block_to_charcoal",
                RecipeType.SHAPELESS,
                java.util.List.of(item(CustomItem.CHARCOAL_BLOCK)),
                new ItemResult("minecraft:charcoal", 9)));
        registerBrowser(new ShapedRecipeDefinition("haohanmetallurgy:charcoal_to_charcoal_block",
                java.util.List.of("CCC", "CCC", "CCC"),
                java.util.Map.of('C', material(Material.CHARCOAL)),
                result(CustomItem.CHARCOAL_BLOCK, 1)));

        registerPickaxeBrowserRecipe(CustomItem.EMBERSTEEL_PICKAXE, CustomItem.EMBERSTEEL_INGOT);
        registerPickaxeBrowserRecipe(CustomItem.MITHRIL_PICKAXE, CustomItem.MITHRIL_INGOT);
        registerPickaxeBrowserRecipe(CustomItem.SOULSTEEL_PICKAXE, CustomItem.SOULSTEEL_INGOT);
        for (Map.Entry<String, CustomItem> entry : PICKAXE_INGREDIENTS.entrySet()) {
            CustomItem result = CustomItem.getById(entry.getKey()).orElse(null);
            if (result != null && entry.getKey().endsWith("_slag_pickaxe")) {
                registerPickaxeBrowserRecipe(result, entry.getValue());
            }
        }
    }

    private void registerPickaxeBrowserRecipe(CustomItem result, CustomItem ingredient) {
        registerBrowser(new ShapedRecipeDefinition("haohanmetallurgy:" + result.getId(),
                java.util.List.of("III", " T ", " T "),
                java.util.Map.of('I', item(ingredient), 'T', material(Material.STICK)),
                result(result, 1)));
    }

    private Ingredient.ItemIngredient item(CustomItem item) {
        return new Ingredient.ItemIngredient("haohanmetallurgy:" + item.getId());
    }

    private Ingredient.MaterialIngredient material(Material material) {
        return new Ingredient.MaterialIngredient(material);
    }

    private Ingredient.MaterialIngredient material(Material material, int amount) {
        return new Ingredient.MaterialIngredient(material, amount);
    }

    private ItemResult result(CustomItem item, int amount) {
        return new ItemResult("haohanmetallurgy:" + item.getId(), amount);
    }

    private void registerBrowser(RecipeDefinition recipe) {
        var registry = HaoHanItemCore.get().getRecipeRegistry();
        if (!registry.exists(recipe.getId())) {
            registry.register(recipe);
        }
        browserRecipeIds.add(recipe.getId());
    }

    /** Publishes Metallurgy machine recipes as read-only entries in Browser. */
    public void registerMachineBrowserRecipes(RecipeLoader loader) {
        for (MetallurgyRecipe recipe : loader.all()) {
            List<Ingredient> ingredients = new java.util.ArrayList<>();
            for (MetallurgyRecipe.Ingredient input : recipe.getInputs()) {
                if (input.customItemId() != null && !input.customItemId().isBlank()) {
                    ingredients.add(new Ingredient.ItemIngredient(
                            "haohanmetallurgy:" + input.customItemId(), input.amount()));
                } else {
                    ingredients.add(new Ingredient.MaterialIngredient(input.material(), input.amount()));
                }
            }

            MetallurgyRecipe.OutputItem output = recipe.getOutput();
            String outputId = output.customItemId() != null && !output.customItemId().isBlank()
                    ? "haohanmetallurgy:" + output.customItemId()
                    : "minecraft:" + output.material().name().toLowerCase(java.util.Locale.ROOT);
            registerBrowser(new RecipeDefinition(
                    "haohanmetallurgy:" + recipe.getId().replace(':', '_'),
                    RecipeType.MACHINE,
                    ingredients,
                    new ItemResult(outputId, output.amount())));
        }
    }

    private void unregisterBrowserRecipes() {
        var registry = HaoHanItemCore.get().getRecipeRegistry();
        browserRecipeIds.forEach(registry::unregister);
        browserRecipeIds.clear();
    }

    private void registerMithrilCompression() {
        ItemStack ingot = plugin.getItemManager().createItem(CustomItem.MITHRIL_INGOT, 1);
        ItemStack shards = plugin.getItemManager().createItem(CustomItem.MITHRIL_SHARD, 9);

        ShapelessRecipe ingotToShards = new ShapelessRecipe(key("mithril_ingot_to_shards"), shards);
        ingotToShards.setCategory(CraftingBookCategory.MISC);
        ingotToShards.addIngredient(exact(ingot));
        addRecipe(ingotToShards);

        ShapedRecipe shardsToIngot = new ShapedRecipe(key("mithril_shards_to_ingot"), ingot);
        shardsToIngot.setCategory(CraftingBookCategory.MISC);
        shardsToIngot.shape("SSS", "SSS", "SSS");
        shardsToIngot.setIngredient('S', exact(plugin.getItemManager().createItem(CustomItem.MITHRIL_SHARD, 1)));
        addRecipe(shardsToIngot);
    }

    private void registerBoraxGrinding() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("raw_borax_to_powder"),
                plugin.getItemManager().createItem(CustomItem.BORAX_POWDER, 1));
        recipe.setCategory(CraftingBookCategory.MISC);
        recipe.addIngredient(CustomItem.RAW_BORAX.getMaterial());
        addRecipe(recipe);
    }

    private void registerBowDrill() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("bow_drill"),
                plugin.getItemManager().createItem(CustomItem.BOW_DRILL, 1));
        recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        recipe.addIngredient(2, Material.STICK);
        recipe.addIngredient(exact(plugin.getItemManager().createItem(CustomItem.JUTE_CORD, 1)));
        addRecipe(recipe);
    }

    private void registerCharcoalBlockUnpacking() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("charcoal_block_to_charcoal"),
                new ItemStack(Material.CHARCOAL, 9));
        recipe.setCategory(CraftingBookCategory.MISC);
        recipe.addIngredient(exact(plugin.getItemManager().createItem(CustomItem.CHARCOAL_BLOCK, 1)));
        addRecipe(recipe);
    }

    private void registerCharcoalBlockPacking() {
        ShapedRecipe recipe = new ShapedRecipe(
                key("charcoal_to_charcoal_block"),
                plugin.getItemManager().createItem(CustomItem.CHARCOAL_BLOCK, 1));
        recipe.setCategory(CraftingBookCategory.BUILDING);
        recipe.shape("CCC", "CCC", "CCC");
        recipe.setIngredient('C', Material.CHARCOAL);
        addRecipe(recipe);
    }

    private void registerAlloyPickaxes() {
        registerAlloyPickaxe(CustomItem.EMBERSTEEL_PICKAXE, CustomItem.EMBERSTEEL_INGOT);
        registerAlloyPickaxe(CustomItem.MITHRIL_PICKAXE, CustomItem.MITHRIL_INGOT);
        registerAlloyPickaxe(CustomItem.SOULSTEEL_PICKAXE, CustomItem.SOULSTEEL_INGOT);
    }

    private void registerAlloyPickaxe(CustomItem result, CustomItem ingredient) {
        ShapedRecipe recipe = new ShapedRecipe(key(result.getId()), plugin.getItemManager().createItem(result, 1));
        recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        recipe.shape("III", " T ", " T ");
        recipe.setIngredient('I', exact(plugin.getItemManager().createItem(ingredient, 1)));
        recipe.setIngredient('T', Material.STICK);
        addRecipe(recipe);
    }

    private void registerSlagPickaxes() {
        for (Map.Entry<String, CustomItem> entry : PICKAXE_INGREDIENTS.entrySet()) {
            CustomItem result = CustomItem.getById(entry.getKey()).orElse(null);
            if (result == null || !entry.getKey().endsWith("_slag_pickaxe")) {
                continue;
            }
            ShapedRecipe recipe = new ShapedRecipe(key(result.getId()), plugin.getItemManager().createItem(result, 1));
            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            recipe.shape("SSS", " T ", " T ");
            recipe.setIngredient('S', exact(plugin.getItemManager().createItem(entry.getValue(), 1)));
            recipe.setIngredient('T', Material.STICK);
            addRecipe(recipe);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (isRecipe(event.getRecipe(), "minecraft", "netherite_ingot")) {
            event.getInventory().setResult(null);
            return;
        }

        ItemStack[] matrix = event.getInventory().getMatrix();
        // Always provide the canonical unpacking preview for the PDC-backed
        // charcoal block, regardless of datapack recipe selection order.
        if (isSingleCustomItem(matrix, CustomItem.CHARCOAL_BLOCK)) {
            event.getInventory().setResult(new ItemStack(Material.CHARCOAL, 9));
            return;
        }

        // Vanilla recipes match only the physical carrier Material (for
        // example Iron Slag is a CLAY_BALL), so without this guard custom
        // items can accidentally make clay blocks, iron blocks, tools, etc.
        if (isMinecraftRecipe(event.getRecipe()) && containsCustomItem(matrix)) {
            event.getInventory().setResult(null);
            return;
        }

        String id = customRecipeId(event.getRecipe());
        if (id == null) {
            return;
        }

        event.getInventory().setResult(isValid(id, matrix) ? createResult(id) : null);
    }

    /** Server-side safety net for normal click, number-key and shift crafting. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        if (isSingleCustomItem(matrix, CustomItem.CHARCOAL_BLOCK)) {
            return;
        }
        if (isMinecraftRecipe(event.getRecipe()) && containsCustomItem(matrix)) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
            return;
        }

        String id = customRecipeId(event.getRecipe());
        if (id != null && !isValid(id, matrix)) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        Recipe recipe = event.getInventory().getRecipe();
        if (recipe instanceof Keyed keyed
                && "minecraft".equals(keyed.getKey().getNamespace())
                && keyed.getKey().getKey().startsWith("netherite_")
                && keyed.getKey().getKey().endsWith("_smithing")) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof Crafter crafter)) {
            return;
        }
        if (isSingleCustomItem(crafter.getInventory().getContents(), CustomItem.CHARCOAL_BLOCK)) {
            event.setResult(new ItemStack(Material.CHARCOAL, 9));
            return;
        }

        if (isMinecraftRecipe(event.getRecipe())
                && containsCustomItem(crafter.getInventory().getContents())) {
            event.setCancelled(true);
            return;
        }

        String id = customRecipeId(event.getRecipe());
        if (id == null) {
            return;
        }

        if (!isValid(id, crafter.getInventory().getContents())) {
            event.setCancelled(true);
            return;
        }
        event.setResult(createResult(id));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> discoverRecipes(event.getPlayer()));
    }

    private boolean isValid(String id, ItemStack[] matrix) {
        if ("mithril_ingot_to_shards".equals(id)) {
            int found = 0;
            for (ItemStack item : matrix) {
                if (!isEmpty(item)) {
                    if (!plugin.getItemManager().isCustomItem(item, CustomItem.MITHRIL_INGOT)) {
                        return false;
                    }
                    found++;
                }
            }
            return found == 1;
        }

        if ("raw_borax_to_powder".equals(id)) {
            int found = 0;
            for (ItemStack item : matrix) {
                if (!isEmpty(item)) {
                    if (!plugin.getItemManager().isCustomItem(item, CustomItem.RAW_BORAX)) return false;
                    found++;
                }
            }
            return found == 1;
        }

        if ("mithril_shards_to_ingot".equals(id)) {
            if (matrix.length != 9) {
                return false;
            }
            for (ItemStack item : matrix) {
                if (!plugin.getItemManager().isCustomItem(item, CustomItem.MITHRIL_SHARD)) {
                    return false;
                }
            }
            return true;
        }

        if ("bow_drill".equals(id)) {
            int sticks = 0;
            int cords = 0;
            for (ItemStack item : matrix) {
                if (isEmpty(item)) continue;
                if (isMaterial(item, Material.STICK)) {
                    sticks++;
                } else if (plugin.getItemManager().isCustomItem(item, CustomItem.JUTE_CORD)) {
                    cords++;
                } else {
                    return false;
                }
            }
            return sticks == 2 && cords == 1;
        }

        if ("charcoal_block_to_charcoal".equals(id)) {
            return isSingleCustomItem(matrix, CustomItem.CHARCOAL_BLOCK);
        }

        if ("charcoal_to_charcoal_block".equals(id)) {
            if (matrix.length != 9) return false;
            for (ItemStack item : matrix) {
                if (!isMaterial(item, Material.CHARCOAL)) return false;
            }
            return true;
        }

        CustomItem ingredient = PICKAXE_INGREDIENTS.get(id);
        if (ingredient == null || matrix.length != 9) {
            return false;
        }
        for (int slot : new int[]{0, 1, 2}) {
            if (!plugin.getItemManager().isCustomItem(matrix[slot], ingredient)) {
                return false;
            }
        }
        if (!isMaterial(matrix[4], Material.STICK) || !isMaterial(matrix[7], Material.STICK)) {
            return false;
        }
        for (int slot : new int[]{3, 5, 6, 8}) {
            if (!isEmpty(matrix[slot])) {
                return false;
            }
        }
        return true;
    }

    private ItemStack createResult(String id) {
        if ("mithril_ingot_to_shards".equals(id)) {
            return plugin.getItemManager().createItem(CustomItem.MITHRIL_SHARD, 9);
        }
        if ("raw_borax_to_powder".equals(id)) {
            return plugin.getItemManager().createItem(CustomItem.BORAX_POWDER, 1);
        }
        if ("mithril_shards_to_ingot".equals(id)) {
            return plugin.getItemManager().createItem(CustomItem.MITHRIL_INGOT, 1);
        }
        if ("charcoal_block_to_charcoal".equals(id)) {
            return new ItemStack(Material.CHARCOAL, 9);
        }
        if ("charcoal_to_charcoal_block".equals(id)) {
            return plugin.getItemManager().createItem(CustomItem.CHARCOAL_BLOCK, 1);
        }
        return CustomItem.getById(id)
                .map(item -> plugin.getItemManager().createItem(item, 1))
                .orElse(null);
    }

    private String customRecipeId(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed) || !recipeKeys.contains(keyed.getKey())) {
            return null;
        }
        return keyed.getKey().getKey();
    }

    private boolean isRecipe(Recipe recipe, String namespace, String value) {
        return recipe instanceof Keyed keyed
                && namespace.equals(keyed.getKey().getNamespace())
                && value.equals(keyed.getKey().getKey());
    }

    private boolean isMaterial(ItemStack item, Material material) {
        return item != null && item.getType() == material;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private boolean isMinecraftRecipe(Recipe recipe) {
        return recipe instanceof Keyed keyed
                && NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace());
    }

    private boolean containsCustomItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (!isEmpty(item) && plugin.getItemManager().getCustomItem(item).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSingleCustomItem(ItemStack[] matrix, CustomItem target) {
        int found = 0;
        for (ItemStack item : matrix) {
            if (isEmpty(item)) continue;
            if (!plugin.getItemManager().isCustomItem(item, target)) return false;
            found++;
        }
        return found == 1;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    private RecipeChoice.MaterialChoice exact(ItemStack item) {
        return new RecipeChoice.MaterialChoice(java.util.List.of(item.getType()));
    }

    private void addRecipe(Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            plugin.getServer().removeRecipe(keyed.getKey());
            recipeKeys.add(keyed.getKey());
        }
        plugin.getServer().addRecipe(recipe);
    }

    private void discoverRecipesForOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            discoverRecipes(player);
        }
    }

    private void discoverRecipes(Player player) {
        player.discoverRecipes(recipeKeys);
    }

}
