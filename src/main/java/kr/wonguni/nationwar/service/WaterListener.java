package kr.wonguni.nationwar.service;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class WaterListener implements Listener {
    private final JavaPlugin plugin;
    private final WaterService water;
    private final NamespacedKey waterTypeKey;
    private final NamespacedKey itemIdKey;

    public WaterListener(JavaPlugin plugin, WaterService water) {
        this.plugin = plugin;
        this.water = water;
        this.waterTypeKey = new NamespacedKey(plugin, "nw_water_type");
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
    }

    private boolean isOceanOrBeach(org.bukkit.block.Biome b) {
        String n = b.name().toLowerCase(java.util.Locale.ROOT);
        return n.contains("ocean") || n.contains("beach");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFill(PlayerBucketFillEvent e) {
        Block b = e.getBlockClicked().getRelative(e.getBlockFace());
        if (b.getType() != Material.WATER) return;

        String type = water.get(b.getLocation());
        if (type == null) {
            type = isOceanOrBeach(b.getBiome()) ? "salt" : "drink";
        }

        ItemStack result = new ItemStack(Material.WATER_BUCKET, 1);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(waterTypeKey, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, type.equals("salt") ? "bucket_salt" : "bucket_drink");
            meta.setDisplayName(type.equals("salt") ? "§f염수 양동이" : "§f식수 양동이");
            result.setItemMeta(meta);
        }
        e.setItemStack(result);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEmpty(PlayerBucketEmptyEvent e) {
        ItemStack bucket = e.getItemStack();
        if (bucket == null || bucket.getType() != Material.WATER_BUCKET) return;
        ItemMeta meta = bucket.getItemMeta();
        if (meta == null) return;
        String type = meta.getPersistentDataContainer().get(waterTypeKey, PersistentDataType.STRING);
        if (type == null) return;

        Block placed = e.getBlockClicked().getRelative(e.getBlockFace());
        // record after placement
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (placed.getType() == Material.WATER) water.record(placed.getLocation(), type);
        });
    }

    // Infinite water prevention: cancel source formation when 2+ neighboring sources exist
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent e) {
        Block from = e.getBlock();
        Block to = e.getToBlock();
        if (from.getType() != Material.WATER) return;
        if (to.getType() != Material.AIR) return;

        if (!(from.getBlockData() instanceof Levelled lv)) return;
        // Only check when flowing (not source)
        if (lv.getLevel() == 0) return;

        int sources = 0;
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.WEST
        }) {
            Block nb = to.getRelative(face);
            if (nb.getType() == Material.WATER && nb.getBlockData() instanceof Levelled nlv && nlv.getLevel() == 0) {
                sources++;
            }
        }
        if (sources >= 2) {
            e.setCancelled(true);
        }
    }
}
