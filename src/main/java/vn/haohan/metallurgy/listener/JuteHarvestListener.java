package vn.haohan.metallurgy.listener;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Drops jute cord when grass is cut with flint. */
public final class JuteHarvestListener implements Listener {
    private static final Set<Material> JUTE_PLANTS = Set.of(
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN);

    private final HaoHanMetallurgy plugin;

    public JuteHarvestListener(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrassCut(BlockBreakEvent event) {
        if (!JUTE_PLANTS.contains(event.getBlock().getType())) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.FLINT) return;

        DropProfile profile = dropProfile(event.getBlock().getType());
        if (ThreadLocalRandom.current().nextDouble() >= profile.chance()) return;

        int amount = profile.minAmount() == profile.maxAmount()
                ? profile.minAmount()
                : ThreadLocalRandom.current().nextInt(profile.minAmount(), profile.maxAmount() + 1);

        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.35, 0.5),
                plugin.getItemManager().createItem(CustomItem.JUTE_CORD, amount));
    }

    private DropProfile dropProfile(Material plant) {
        DropProfile defaults = switch (plant) {
            case SHORT_GRASS -> new DropProfile(0.18, 1, 1);
            case FERN -> new DropProfile(0.20, 1, 1);
            case TALL_GRASS -> new DropProfile(0.30, 1, 2);
            case LARGE_FERN -> new DropProfile(0.32, 1, 2);
            default -> new DropProfile(0.0, 1, 1);
        };

        String path = "jute.drops." + plant.name() + ".";
        double chance = Math.max(0.0, Math.min(1.0,
                plugin.getConfig().getDouble(path + "chance", defaults.chance())));
        int minAmount = Math.max(1,
                plugin.getConfig().getInt(path + "min-amount", defaults.minAmount()));
        int maxAmount = Math.max(minAmount,
                plugin.getConfig().getInt(path + "max-amount", defaults.maxAmount()));
        return new DropProfile(chance, minAmount, maxAmount);
    }

    private record DropProfile(double chance, int minAmount, int maxAmount) {}
}
