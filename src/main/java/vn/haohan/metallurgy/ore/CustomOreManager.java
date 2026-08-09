package vn.haohan.metallurgy.ore;

import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import vn.haohan.metallurgy.item.CustomOreBlock;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stores custom block locations and assigns reserved NoteBlock model states. */
public final class CustomOreManager {
    private final HaoHanMetallurgy plugin;
    private final NamespacedKey displayKey;
    private final Map<OreKey, CustomItem> ores = new LinkedHashMap<>();

    public CustomOreManager(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
        this.displayKey = new NamespacedKey(plugin, "custom_ore_display");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::reconcileLoadedOres, 20L, 20L);
    }

    public CustomItem getOre(Block block) {
        if (block == null) return null;
        return ores.get(OreKey.from(block));
    }

    public void register(Block block, CustomItem item) {
        Material carrier = CustomOreBlock.carrierFor(item);
        if (carrier == null) throw new IllegalArgumentException("Not a managed custom block: " + item);

        block.setBlockData(CustomOreBlock.blockDataFor(item), false);
        ores.put(OreKey.from(block), item);
        removeLegacyDisplay(block.getChunk(), OreKey.from(block).serialized());
        saveAll();
    }

    public CustomItem unregister(Block block) {
        OreKey key = OreKey.from(block);
        CustomItem removed = ores.remove(key);
        if (removed != null) {
            removeLegacyDisplay(block.getChunk(), key.serialized());
            saveAll();
        }
        return removed;
    }

    public void loadAll() {
        ores.clear();
        File file = dataFile();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("ores");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String path = "ores." + id;
            World world = Bukkit.getWorld(yaml.getString(path + ".world", ""));
            CustomItem item = CustomItem.getById(yaml.getString(path + ".type", "")).orElse(null);
            if (world == null || !CustomOreBlock.isManagedBlock(item)) continue;

            int x = yaml.getInt(path + ".x");
            int y = yaml.getInt(path + ".y");
            int z = yaml.getInt(path + ".z");
            OreKey key = new OreKey(world.getName(), x, y, z);
            if (world.isChunkLoaded(x >> 4, z >> 4)) {
                Block block = world.getBlockAt(x, y, z);
                if (CustomOreBlock.matches(block, item)) {
                    ores.put(key, item);
                    removeLegacyDisplay(block.getChunk(), key.serialized());
                } else if (block.getType() == CustomOreBlock.legacyCarrierFor(item)) {
                    ores.put(key, item);
                    block.setBlockData(CustomOreBlock.blockDataFor(item), false);
                    removeLegacyDisplay(block.getChunk(), key.serialized());
                }
            } else {
                ores.put(key, item);
            }
        }
        plugin.getPluginLogger().info("Loaded " + ores.size() + " custom ore block(s).");
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        int index = 0;
        for (var entry : ores.entrySet()) {
            OreKey key = entry.getKey();
            String path = "ores." + index++;
            yaml.set(path + ".world", key.world());
            yaml.set(path + ".x", key.x());
            yaml.set(path + ".y", key.y());
            yaml.set(path + ".z", key.z());
            yaml.set(path + ".type", entry.getValue().getId());
        }
        try {
            yaml.save(dataFile());
        } catch (IOException e) {
            plugin.getPluginLogger().error("Failed to save custom_ores.yml", e);
        }
    }

    public void onChunkLoad(Chunk chunk) {
        boolean changed = false;
        for (var entry : new java.util.ArrayList<>(ores.entrySet())) {
            OreKey key = entry.getKey();
            if (!key.inChunk(chunk)) continue;

            Block block = chunk.getWorld().getBlockAt(key.x(), key.y(), key.z());
            if (block.getType() == CustomOreBlock.legacyCarrierFor(entry.getValue())) {
                block.setBlockData(CustomOreBlock.blockDataFor(entry.getValue()), false);
            } else if (!CustomOreBlock.matches(block, entry.getValue())) {
                ores.remove(key);
                changed = true;
            }
            removeLegacyDisplay(chunk, key.serialized());
        }
        if (changed) saveAll();
    }

    public void onChunkUnload(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(displayKey, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    public void shutdown() {
        saveAll();
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(displayKey, PersistentDataType.STRING)) {
                    display.remove();
                }
            }
        }
    }

    /** Removes stale registry entries after fill/WorldEdit edits that bypass Bukkit events. */
    public void reconcileLoadedOres() {
        boolean changed = false;
        for (var entry : new java.util.ArrayList<>(ores.entrySet())) {
            OreKey key = entry.getKey();
            World world = Bukkit.getWorld(key.world());
            if (world == null || !world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) continue;

            Block block = world.getBlockAt(key.x(), key.y(), key.z());
            if (!CustomOreBlock.matches(block, entry.getValue())) {
                ores.remove(key);
                removeLegacyDisplay(block.getChunk(), key.serialized());
                changed = true;
            }
        }
        if (changed) saveAll();
    }

    private void removeLegacyDisplay(Chunk chunk, String serializedKey) {
        for (Entity entity : chunk.getEntities()) {
            String value = entity.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING);
            if (serializedKey.equals(value)) entity.remove();
        }
    }

    private File dataFile() {
        return new File(plugin.getDataFolder(), "custom_ores.yml");
    }

    private record OreKey(String world, int x, int y, int z) {
        static OreKey from(Block block) {
            return new OreKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }

        String serialized() {
            return world + ":" + x + ":" + y + ":" + z;
        }

        boolean inChunk(Chunk chunk) {
            return world.equals(chunk.getWorld().getName())
                    && (x >> 4) == chunk.getX() && (z >> 4) == chunk.getZ();
        }
    }
}
