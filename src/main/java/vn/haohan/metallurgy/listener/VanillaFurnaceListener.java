package vn.haohan.metallurgy.listener;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Metallurgy behaviour for the two native vanilla furnace screens. */
public class VanillaFurnaceListener implements Listener {

    private static final int COOK_TIME_MULTIPLIER = 5;
    private static final int FUEL_BURN_DIVIDER = 5;

    private static final Set<Material> COPPER_INPUTS = Set.of(
            Material.RAW_COPPER, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
    private static final Set<Material> IRON_INPUTS = Set.of(
            Material.RAW_IRON, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
    private static final Set<Material> GOLD_INPUTS = Set.of(
            Material.RAW_GOLD, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE);
    private static final Set<Material> NETHERITE_INPUTS = Set.of(Material.ANCIENT_DEBRIS);
    private static final Set<Material> CONTROLLED_INPUTS = Set.of(
            Material.RAW_COPPER, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.RAW_IRON, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.RAW_GOLD, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS);

    private final HaoHanMetallurgy plugin;
    private final NamespacedKey legacyStoredSlagKey;

    public VanillaFurnaceListener(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
        this.legacyStoredSlagKey = new NamespacedKey(plugin, "vanilla_furnace_slag");
    }

    /** Replaces controlled vanilla recipes with predictable clean-metal recipes. */
    public void registerMetalRecipes() {
        removeVanillaMetalRecipes();
        registerMetalRecipes("copper", COPPER_INPUTS, Material.COPPER_INGOT);
        registerMetalRecipes("iron", IRON_INPUTS, Material.IRON_INGOT);
        registerMetalRecipes("gold", GOLD_INPUTS, Material.GOLD_INGOT);
        registerMetalRecipes("netherite", NETHERITE_INPUTS, Material.NETHERITE_SCRAP);
    }

    @Deprecated
    public void registerSlagRecipes() {
        registerMetalRecipes();
    }

    private void removeVanillaMetalRecipes() {
        List<NamespacedKey> recipeKeys = new ArrayList<>();
        java.util.Iterator<Recipe> recipes = plugin.getServer().recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (!(recipe instanceof CookingRecipe<?> cooking)
                    || !(recipe instanceof Keyed keyed)
                    || !NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace())) {
                continue;
            }
            RecipeChoice inputChoice = cooking.getInputChoice();
            if (CONTROLLED_INPUTS.stream().anyMatch(m -> inputChoice.test(new ItemStack(m)))) {
                recipeKeys.add(keyed.getKey());
            }
        }
        recipeKeys.forEach(plugin.getServer()::removeRecipe);
    }

    private void registerMetalRecipes(String id, Set<Material> inputs, Material cleanResult) {
        ItemStack result = new ItemStack(cleanResult);
        RecipeChoice.MaterialChoice inputChoice = new RecipeChoice.MaterialChoice(new ArrayList<>(inputs));

        NamespacedKey smeltingKey = new NamespacedKey(plugin, id + "_metal_from_smelting");
        plugin.getServer().removeRecipe(smeltingKey);
        plugin.getServer().addRecipe(new FurnaceRecipe(
                smeltingKey, result, inputChoice, 0.0f, 200));

        NamespacedKey blastingKey = new NamespacedKey(plugin, id + "_metal_from_blasting");
        plugin.getServer().removeRecipe(blastingKey);
        plugin.getServer().addRecipe(new BlastingRecipe(
                blastingKey, result, inputChoice, 0.0f, 100));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        if (isControlled(event.getSource().getType())) {
            event.setTotalCookTime(event.getTotalCookTime() * COOK_TIME_MULTIPLIER);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!(event.getBlock().getState() instanceof Furnace furnace)) return;
        ItemStack source = furnace.getInventory().getSmelting();
        if (source != null && isControlled(source.getType())) {
            event.setBurnTime(Math.max(1, event.getBurnTime() / FUEL_BURN_DIVIDER));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Material source = event.getSource().getType();
        if (source == Material.DEBUG_STICK
                || (event.getResult().getType() == Material.NETHERITE_SCRAP
                && !NETHERITE_INPUTS.contains(source))) {
            event.setCancelled(true);
            return;
        }

        CustomItem slagType = slagFor(source);
        Material cleanType = cleanFor(source);
        if (slagType == null || cleanType == null) return;

        // Keep the registry/result slot typed as clean metal so vanilla's
        // canBurn check continues processing the full input stack.
        event.setResult(new ItemStack(cleanType));
        boolean clean = ThreadLocalRandom.current().nextDouble()
                < plugin.getConfigManager().getVanillaFurnaceCleanOutputChance();
        if (clean) return;

        ItemStack slag = plugin.getItemManager().createItem(slagType, 1);
        Location location = event.getBlock().getLocation();
        // The event fires before vanilla inserts its temporary clean result.
        // Replace it on the next tick and eject slag in front of the furnace.
        Bukkit.getScheduler().runTask(plugin,
                () -> ejectLastResultAsSlag(location, cleanType, slag));
        event.getBlock().getWorld().playSound(
                location, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
    }

    private void ejectLastResultAsSlag(Location location, Material cleanType, ItemStack slag) {
        if (!(location.getBlock().getState() instanceof Furnace furnace)) return;
        ItemStack result = furnace.getInventory().getResult();
        if (result != null && result.getType() == cleanType && result.getAmount() > 0) {
            if (result.getAmount() == 1) furnace.getInventory().setResult(null);
            else result.setAmount(result.getAmount() - 1);
        }

        location.getWorld().dropItem(ejectionLocation(location), slag);
    }

    /** Ejects slag left behind by the removed two-output custom GUI. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLegacyFurnaceOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) return;
        Material type = event.getClickedBlock().getType();
        if (type != Material.FURNACE && type != Material.BLAST_FURNACE) return;
        Location location = event.getClickedBlock().getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> ejectLegacyStoredSlag(location, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLegacyFurnaceBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.FURNACE || type == Material.BLAST_FURNACE) {
            ejectLegacyStoredSlag(event.getBlock().getLocation(), false);
        }
    }

    private void ejectLegacyStoredSlag(Location location, boolean updateState) {
        if (!(location.getBlock().getState() instanceof Furnace furnace)) return;
        byte[] bytes = furnace.getPersistentDataContainer().get(
                legacyStoredSlagKey, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) return;

        ItemStack slag;
        try {
            slag = ItemStack.deserializeBytes(bytes);
        } catch (IllegalArgumentException exception) {
            plugin.getPluginLogger().error("Invalid legacy furnace slag at " + location, exception);
            return;
        }
        furnace.getPersistentDataContainer().remove(legacyStoredSlagKey);
        if (updateState) {
            org.bukkit.block.data.BlockData liveData = location.getBlock().getBlockData().clone();
            furnace.update(true, false);
            location.getBlock().setBlockData(liveData, false);
        }
        location.getWorld().dropItem(ejectionLocation(location), slag);
    }

    private Location ejectionLocation(Location location) {
        Location dropLocation = location.clone().add(0.5, 0.35, 0.5);
        if (location.getBlock().getBlockData() instanceof Directional directional) {
            dropLocation.add(directional.getFacing().getDirection().multiply(0.8));
        }
        return dropLocation;
    }

    private CustomItem slagFor(Material source) {
        if (COPPER_INPUTS.contains(source)) return CustomItem.COPPER_SLAG;
        if (IRON_INPUTS.contains(source)) return CustomItem.IRON_SLAG;
        if (GOLD_INPUTS.contains(source)) return CustomItem.GOLD_SLAG;
        if (NETHERITE_INPUTS.contains(source)) return CustomItem.NETHERITE_SLAG;
        return null;
    }

    private Material cleanFor(Material source) {
        if (COPPER_INPUTS.contains(source)) return Material.COPPER_INGOT;
        if (IRON_INPUTS.contains(source)) return Material.IRON_INGOT;
        if (GOLD_INPUTS.contains(source)) return Material.GOLD_INGOT;
        if (NETHERITE_INPUTS.contains(source)) return Material.NETHERITE_SCRAP;
        return null;
    }

    private boolean isControlled(Material material) {
        return CONTROLLED_INPUTS.contains(material) || material == Material.DEBUG_STICK;
    }
}
