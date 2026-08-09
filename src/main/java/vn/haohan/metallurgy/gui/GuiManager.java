package vn.haohan.metallurgy.gui;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.*;

public class GuiManager implements Listener {

    private final HaoHanMetallurgy plugin;

    private final Map<UUID, MetallurgyGui> openGuis = new HashMap<>();

    public GuiManager(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, MetallurgyGui gui) {
        openGuis.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    public void close(Player player) {
        openGuis.remove(player.getUniqueId());
        player.closeInventory();
    }

    public boolean hasOpenGui(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    public Optional<MetallurgyGui> getGui(Player player) {
        return Optional.ofNullable(openGuis.get(player.getUniqueId()));
    }

    public int count() {
        return openGuis.size();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        MetallurgyGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        if (!event.getInventory().equals(gui.getInventory())) return;

        event.setCancelled(true);
        gui.onClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        MetallurgyGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;
        if (!event.getInventory().equals(gui.getInventory())) return;

        int guiSize = gui.getInventory().getSize();
        boolean touchesGui = event.getRawSlots().stream().anyMatch(s -> s < guiSize);
        if (touchesGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        MetallurgyGui gui = openGuis.remove(player.getUniqueId());
        if (gui != null) {
            gui.onClose(event);
            plugin.getPluginLogger().debug("Closed GUI for " + player.getName());
        }
    }
}
