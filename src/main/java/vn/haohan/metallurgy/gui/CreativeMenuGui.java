package vn.haohan.metallurgy.gui;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Arrays;
import java.util.Comparator;

public class CreativeMenuGui extends MetallurgyGui {

    private static final int PAGE_SIZE = 27;
    private static final int PREVIOUS_SLOT = 27;
    private static final int PAGE_SLOT = 30;
    private static final int CLOSE_SLOT = 31;
    private static final int NEXT_SLOT = 35;

    private int currentPage;

    public CreativeMenuGui(HaoHanMetallurgy plugin) {
        super(plugin);
    }

    @Override
    protected void buildLayout() {
        this.inventory = Bukkit.createInventory(null, 36, plugin.getLanguageManager().text("menu.title"));
    }

    @Override
    public void refresh() {
        if (inventory == null) return;
        inventory.clear();

        List<CustomItem> items = sortedItems();
        int pageCount = Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        currentPage = Math.max(0, Math.min(currentPage, pageCount - 1));
        int startIndex = currentPage * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && startIndex + slot < items.size(); slot++) {
            inventory.setItem(slot, plugin.getItemManager().createGuiItem(items.get(startIndex + slot), 1));
        }

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7");
            pane.setItemMeta(meta);
        }
        for (int i = PAGE_SIZE; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane);
        }

        if (currentPage > 0) {
            inventory.setItem(PREVIOUS_SLOT, makeButton(Material.ARROW, "§e← Trang trước"));
        }
        inventory.setItem(PAGE_SLOT, makeButton(Material.BOOK,
                "§fTrang §e" + (currentPage + 1) + "§7/§e" + pageCount));
        if (currentPage + 1 < pageCount) {
            inventory.setItem(NEXT_SLOT, makeButton(Material.ARROW, "§eTrang sau →"));
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(plugin.getLanguageManager().text("menu.close"));
            closeMeta.setLore(List.of(plugin.getLanguageManager().text("menu.close-lore")));
            close.setItemMeta(closeMeta);
        }
        inventory.setItem(CLOSE_SLOT, close);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        List<CustomItem> items = sortedItems();
        int pageCount = Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (slot == PREVIOUS_SLOT && currentPage > 0) {
            currentPage--;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            return;
        }
        if (slot == NEXT_SLOT && currentPage + 1 < pageCount) {
            currentPage++;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            return;
        }
        if (slot >= PAGE_SIZE) {
            return;
        }

        int itemIndex = currentPage * PAGE_SIZE + slot;
        if (itemIndex >= 0 && itemIndex < items.size()) {
            CustomItem customItem = items.get(itemIndex);
            
            int amount = 1;
            if (!customItem.getId().endsWith("_pickaxe") && event.isShiftClick()) {
                amount = 64;
            }

            ItemStack given = plugin.getItemManager().createItem(customItem, amount);

            var remaining = player.getInventory().addItem(given);
            if (!remaining.isEmpty()) {
                for (ItemStack item : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }

    private List<CustomItem> sortedItems() {
        return Arrays.stream(CustomItem.values())
                .sorted(Comparator.comparingInt(this::menuCategory))
                .toList();
    }

    private int menuCategory(CustomItem item) {
        String id = item.getId();
        if (id.endsWith("_ore")) return 0;
        if (id.equals("raw_borax") || id.endsWith("_shard") || id.endsWith("_crystal")) return 1;
        if (id.endsWith("_powder")) return 2;
        if (id.endsWith("_ingot")) return 3;
        if (id.endsWith("_slag")) return 4;
        if (id.endsWith("_pickaxe")) return 5;
        return 6;
    }

    private ItemStack makeButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
