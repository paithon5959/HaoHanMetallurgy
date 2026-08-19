package vn.haohan.metallurgy.progression;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.recipe.MetallurgyRecipe;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Plugin-owned progression with a compatibility bridge for legacy advancements. */
public final class ProgressionManager {
    private final NamespacedKey progressKey;

    public ProgressionManager(HaoHanMetallurgy plugin) {
        this.progressKey = new NamespacedKey(plugin, "progression");
    }

    public boolean hasCompleted(Player player, String id) {
        if (player == null || id == null || id.isBlank()) return true;
        String token = token(id);
        if (read(player).contains(token)) return true;

        // Migrate progress if the old datapack is still loaded during upgrade.
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key != null) {
            var advancement = Bukkit.getAdvancement(key);
            if (advancement != null && player.getAdvancementProgress(advancement).isDone()) {
                grant(player, id);
                return true;
            }
        }
        return false;
    }

    public void grant(Player player, String id) {
        if (player == null || id == null || id.isBlank()) return;
        Set<String> unlocked = read(player);
        unlocked.add(token(id));
        player.getPersistentDataContainer().set(
                progressKey, PersistentDataType.STRING, String.join(",", unlocked));

        // Keep visible vanilla advancements working while the legacy pack exists.
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) return;
        var advancement = Bukkit.getAdvancement(key);
        if (advancement == null) return;
        var progress = player.getAdvancementProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) progress.awardCriteria(criterion);
    }

    public void grantForRecipe(UUID playerId, MetallurgyRecipe recipe) {
        if (playerId == null || recipe == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        String outputId = recipe.getOutput().customItemId();
        if (outputId == null && recipe.getOutput().material() != null) {
            outputId = recipe.getOutput().material().name().toLowerCase(java.util.Locale.ROOT);
        }
        String progressionId = switch (outputId == null ? "" : outputId) {
            case "embersteel_ingot" -> "forge_embersteel_ingot";
            case "mithril_ingot" -> "forge_mithril_ingot";
            case "soulsteel_ingot" -> "forge_soulsteel_ingot";
            case "netherite_ingot" -> "forge_netherite_ingot";
            default -> null;
        };
        if (progressionId != null) grant(player, "haohan:metallurgy/" + progressionId);
    }

    private Set<String> read(Player player) {
        String raw = player.getPersistentDataContainer().get(progressKey, PersistentDataType.STRING);
        Set<String> result = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) return result;
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        return result;
    }

    private String token(String id) {
        int slash = id.lastIndexOf('/');
        return (slash >= 0 ? id.substring(slash + 1) : id).toLowerCase(java.util.Locale.ROOT);
    }
}
