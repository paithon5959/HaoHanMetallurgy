package vn.haohan.metallurgy.guide;

import org.bukkit.Material;

/** Item identity used by the guide without storing mutable ItemStack instances. */
public record GuideIcon(Material material, String customItemId) {
    public GuideIcon {
        if (material == null || material == Material.AIR
                || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            throw new IllegalArgumentException("guide icon material must be a non-air item");
        }
        customItemId = customItemId == null || customItemId.isBlank() ? null : customItemId;
    }

    public static GuideIcon vanilla(Material material) {
        return new GuideIcon(material, null);
    }

    public static GuideIcon custom(Material material, String customItemId) {
        return new GuideIcon(material, customItemId);
    }
}
