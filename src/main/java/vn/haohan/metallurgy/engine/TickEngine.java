package vn.haohan.metallurgy.engine;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TickEngine {

    private final HaoHanMetallurgy plugin;
    private BukkitTask task;
    private boolean running = false;

    public TickEngine(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (running) {
            plugin.getPluginLogger().warn("TickEngine is already running!");
            return;
        }

        int tickRate = plugin.getConfigManager().getTickRate();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getMachineManager().tickAll();
                } catch (Exception e) {
                    plugin.getPluginLogger().error("Critical error in TickEngine!", e);
                }
            }
        }.runTaskTimer(plugin, tickRate, tickRate);

        running = true;
        plugin.getPluginLogger().info("TickEngine started (rate=" + tickRate + " ticks).");
    }

    public void stop() {
        if (!running || task == null) return;

        task.cancel();
        running = false;
        plugin.getPluginLogger().info("TickEngine stopped.");
    }

    /**
     * Restart engine (dùng sau khi reload config để áp dụng tick rate mới).
     */
    public void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return running;
    }
}
