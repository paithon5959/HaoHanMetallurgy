package vn.haohan.metallurgy.item;

import vn.haohan.itemcore.api.item.ItemComponent;
import vn.haohan.metallurgy.config.CustomItemStats;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

/** Applies the same configurable stats used by the legacy Metallurgy factory. */
record MetallurgyStatsComponent(CustomItem item, CustomItemStats stats) implements ItemComponent {

    @Override
    public void apply(ItemStack stack, vn.haohan.itemcore.api.item.ItemDefinition definition) {
        var meta = stack.getItemMeta();
        if (meta == null) return;

        // Keep stacks created by /im give compatible with Metallurgy listeners
        // and with stacks created by /met give.
        meta.getPersistentDataContainer().set(
                new NamespacedKey("haohanmetallurgy", "custom_item_id"),
                PersistentDataType.STRING, item.getId());

        if (stats.maxDamage() > 0 && meta instanceof Damageable damageable) {
            damageable.setMaxDamage(stats.maxDamage());
        }
        add(meta, Attribute.ATTACK_DAMAGE, "attack_damage", stats.attackDamage());
        add(meta, Attribute.ATTACK_SPEED, "attack_speed", stats.attackSpeed());
        add(meta, Attribute.MINING_EFFICIENCY, "mining_efficiency", stats.miningEfficiency());
        add(meta, Attribute.BLOCK_BREAK_SPEED, "block_break_speed", stats.blockBreakSpeed());
        stack.setItemMeta(meta);
    }

    private void add(org.bukkit.inventory.meta.ItemMeta meta, Attribute attribute,
                     String suffix, double amount) {
        if (amount == 0.0) return;
        NamespacedKey key = new NamespacedKey("haohanmetallurgy", item.getId() + "_" + suffix);
        meta.addAttributeModifier(attribute, new AttributeModifier(
                key, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
    }
}
