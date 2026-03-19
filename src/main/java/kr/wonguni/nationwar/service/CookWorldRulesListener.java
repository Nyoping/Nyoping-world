package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.JobType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public class CookWorldRulesListener implements Listener {
    private final JavaPlugin plugin;
    private final JobService jobs;
    private final NamespacedKey ownerKey;

    private static final Set<Material> LOCK_BLOCKS = EnumSet.of(
            Material.CAULDRON, Material.WATER_CAULDRON,
            Material.SMOKER, Material.BREWING_STAND, Material.BARREL
    );

    public CookWorldRulesListener(JavaPlugin plugin, JobService jobs) {
        this.plugin = plugin;
        this.jobs = jobs;
        this.ownerKey = new NamespacedKey(plugin, "nw_owner");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!LOCK_BLOCKS.contains(b.getType())) return;
        if (!jobs.getJobs(e.getPlayer().getUniqueId()).contains(JobType.COOK)) return;
        ((TileState)b.getState()).getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, e.getPlayer().getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (!LOCK_BLOCKS.contains(b.getType())) return;
        String owner = ((TileState)b.getState()).getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner == null) return;
        Player p = e.getPlayer();
        if (!owner.equals(p.getUniqueId().toString())) {
            e.setCancelled(true);
            p.sendMessage("§c[요리] §f설치자 전용 조리도구입니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPipe(InventoryMoveItemEvent e) {
        if (e.getSource() != null && e.getSource().getLocation() != null) {
            Block b = e.getSource().getLocation().getBlock();
            if (LOCK_BLOCKS.contains(b.getType()) && ((TileState)b.getState()).getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                e.setCancelled(true);
                return;
            }
        }
        if (e.getDestination() != null && e.getDestination().getLocation() != null) {
            Block b = e.getDestination().getLocation().getBlock();
            if (LOCK_BLOCKS.contains(b.getType()) && ((TileState)b.getState()).getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.CAMPFIRE && b.getType() != Material.SOUL_CAMPFIRE) return;
        scheduleExtinguish(b);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCampfirePlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (b.getType() != Material.CAMPFIRE && b.getType() != Material.SOUL_CAMPFIRE) return;
        if (b.getBlockData() instanceof Campfire c && c.isLit()) scheduleExtinguish(b);
    }

    private void scheduleExtinguish(Block b) {
        int mins = plugin.getConfig().getInt("cook.campfire_auto_extinguish_minutes", 10);
        long delay = 20L * 60L * Math.max(1, mins);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (b.getBlockData() instanceof Campfire c && c.isLit()) {
                    c.setLit(false);
                    b.setBlockData(c, false);
                }
            } catch (Exception ignored) {}
        }, delay);
    }
}
