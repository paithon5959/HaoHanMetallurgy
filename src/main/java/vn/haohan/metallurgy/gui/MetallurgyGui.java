package vn.haohan.metallurgy.gui;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

public abstract class MetallurgyGui {

    protected final HaoHanMetallurgy plugin;
    protected Inventory inventory;

    private BukkitTask refreshTask;

    protected MetallurgyGui(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
    }

    protected abstract void buildLayout();

    public abstract void refresh();

    public void open(Player player) {
        if (inventory == null) buildLayout();
        refresh();
        player.openInventory(inventory);
        plugin.getPluginLogger().debug("Opened " + getClass().getSimpleName() + " for " + player.getName());
    }

    public abstract void onClick(InventoryClickEvent event);

    public void onClose(InventoryCloseEvent event) {
        stopRefreshTask();
    }

    protected final void startRefreshTask(long intervalTicks) {
        stopRefreshTask();
        refreshTask = plugin.getServer().getScheduler()
            .runTaskTimer(plugin, this::refresh, intervalTicks, intervalTicks);
    }

    protected final void stopRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
        }
        refreshTask = null;
    }

    protected void setSlot(int slot, org.bukkit.inventory.ItemStack item) {
        if (inventory != null) inventory.setItem(slot, item);
    }

    public Inventory getInventory() { return inventory; }
}
