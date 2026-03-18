/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Slime
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 */
package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.service.ProtectionService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener
implements Listener {
    private final ProtectionService protection;

    public ProtectionListener(ProtectionService protection) {
        this.protection = protection;
    }

    @EventHandler(ignoreCancelled=true)
    public void onBreak(BlockBreakEvent e) {
        Block b;
        Player p = e.getPlayer();
        if (!this.protection.canModify(p, b = e.getBlock())) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uc774 \uad6c\uc5ed\uc740 \ubcf4\ud638\ub418\uace0 \uc788\uc2b5\ub2c8\ub2e4.");
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (!e.isCancelled()) {
            Location loc = b.getLocation().add(0.5, 0.5, 0.5);
            for (Entity ent : loc.getWorld().getNearbyEntities(loc, 4.0, 6.0, 4.0)) {
                Slime slime;
                if (!(ent instanceof Slime) || !(slime = (Slime)ent).getScoreboardTags().contains("NW_STRUCTURE")) continue;
                Location c = slime.getLocation();
                double dx = Math.abs(loc.getX() - c.getX());
                double dy = Math.abs(loc.getY() - c.getY());
                double dz = Math.abs(loc.getZ() - c.getZ());
                if (!(dx <= 2.0) || !(dy <= 2.0) || !(dz <= 2.0)) continue;
                e.setCancelled(true);
                p.sendMessage("\u00a7c\ub125\uc11c\uc2a4/\uad6c\uc870\ubb3c \uc601\uc5ed \uc548\uc5d0\ub294 \ube14\ub85d\uc744 \uc124\uce58\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
        }
        if (!this.protection.canModify(p, b)) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uc774 \uad6c\uc5ed\uc740 \ubcf4\ud638\ub418\uace0 \uc788\uc2b5\ub2c8\ub2e4.");
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        Player p = e.getPlayer();
        if (!this.protection.canInteract(p, e.getClickedBlock().getLocation())) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uc774 \uad6c\uc5ed\uc740 \ubcf4\ud638\ub418\uace0 \uc788\uc2b5\ub2c8\ub2e4.");
        }
    }
}

