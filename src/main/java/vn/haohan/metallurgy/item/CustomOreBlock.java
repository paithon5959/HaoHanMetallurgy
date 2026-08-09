package vn.haohan.metallurgy.item;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;

/** Shared physical carrier mapping for placeable custom ores. */
public final class CustomOreBlock {
    private CustomOreBlock() {}

    public static boolean isOre(CustomItem item) {
        if (item == null) return false;
        return switch (item) {
            case BORAX_ORE, MITHRIL_ORE, DEEPSLATE_BORAX_ORE, DEEPSLATE_MITHRIL_ORE -> true;
            default -> false;
        };
    }

    public static boolean isManagedBlock(CustomItem item) {
        return carrierFor(item) != null;
    }

    public static Material carrierFor(CustomItem item) {
        return noteFor(item) > 0 ? Material.NOTE_BLOCK : null;
    }

    public static Material legacyCarrierFor(CustomItem item) {
        if (item == null) return null;
        return switch (item) {
            case BORAX_ORE, MITHRIL_ORE -> Material.STONE;
            case DEEPSLATE_BORAX_ORE, DEEPSLATE_MITHRIL_ORE -> Material.DEEPSLATE;
            case CHARCOAL_BLOCK -> Material.COAL_BLOCK;
            default -> null;
        };
    }

    public static BlockData blockDataFor(CustomItem item) {
        int note = noteFor(item);
        if (note <= 0) throw new IllegalArgumentException("Not a managed custom block: " + item);
        NoteBlock data = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        data.setInstrument(Instrument.CUSTOM_HEAD);
        data.setNote(new Note(note));
        data.setPowered(false);
        return data;
    }

    public static boolean matches(Block block, CustomItem item) {
        return block != null && matches(block.getBlockData(), item);
    }

    public static boolean matches(BlockData blockData, CustomItem item) {
        if (!(blockData instanceof NoteBlock data)) return false;
        return data.getInstrument() == Instrument.CUSTOM_HEAD
                && data.getNote().getId() == noteFor(item);
    }

    private static int noteFor(CustomItem item) {
        if (item == null) return -1;
        return switch (item) {
            case BORAX_ORE -> 20;
            case MITHRIL_ORE -> 21;
            case DEEPSLATE_BORAX_ORE -> 22;
            case DEEPSLATE_MITHRIL_ORE -> 23;
            case CHARCOAL_BLOCK -> 24;
            default -> -1;
        };
    }
}
