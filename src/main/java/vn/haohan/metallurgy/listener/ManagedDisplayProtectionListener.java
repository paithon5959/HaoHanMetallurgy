package vn.haohan.metallurgy.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/** Keeps plugin-owned display entities out of an unfiltered /kill @e. */
public final class ManagedDisplayProtectionListener implements Listener {
    public static final String MANAGED_TAG = "haohan_managed";

    private static final String PLAYER_REPLACEMENT = "/kill @e[tag=!" + MANAGED_TAG + "]";
    private static final String SERVER_REPLACEMENT = "kill @e[tag=!" + MANAGED_TAG + "]";

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isUnfilteredKill(event.getMessage(), true)) {
            event.setMessage(PLAYER_REPLACEMENT);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        if (isUnfilteredKill(event.getCommand(), false)) {
            event.setCommand(SERVER_REPLACEMENT);
        }
    }

    private boolean isUnfilteredKill(String command, boolean leadingSlash) {
        String prefix = leadingSlash ? "/" : "";
        return command.matches("(?i)^" + prefix + "(?:minecraft:)?kill\\s+@e\\s*$");
    }
}
