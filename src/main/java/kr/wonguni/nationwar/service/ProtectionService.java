/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.service.NationService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ProtectionService {
    private final JavaPlugin plugin;
    private final NationService nationService;

    public ProtectionService(JavaPlugin plugin, NationService nationService) {
        this.plugin = plugin;
        this.nationService = nationService;
    }

    public boolean isInSpawnProtection(Location loc) {
        double dz;
        if (loc == null) {
            return false;
        }
        String w = this.plugin.getConfig().getString("spawn-protection.world", "world");
        World world = loc.getWorld();
        if (world == null || !world.getName().equalsIgnoreCase(w)) {
            return false;
        }
        double cx = this.plugin.getConfig().getDouble("spawn-protection.center-x", 0.0);
        double cz = this.plugin.getConfig().getDouble("spawn-protection.center-z", 0.0);
        double r = this.plugin.getConfig().getDouble("spawn-protection.radius", 10.0);
        double dx = loc.getX() - cx;
        return dx * dx + (dz = loc.getZ() - cz) * dz <= r * r;
    }

    public Nation getTerritoryOwner(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        int baseX = this.plugin.getConfig().getInt("nation.territory.size-x", 16);
        int baseZ = this.plugin.getConfig().getInt("nation.territory.size-z", 16);
        int minY = this.plugin.getConfig().getInt("nation.territory.min-y", 0);
        int maxY = this.plugin.getConfig().getInt("nation.territory.max-y", 320);
        int y = loc.getBlockY();
        if (y < minY || y > maxY) {
            return null;
        }
        for (Nation n : this.nationService.getAllNations()) {
            if (!n.hasNexus() || n.getNexusWorld() == null || !n.getNexusWorld().equalsIgnoreCase(loc.getWorld().getName())) continue;
            int cx = n.getNexusX();
            int cz = n.getNexusZ();
            int level = Math.max(0, n.getLevel());
            int sx = baseX + level * 2;
            int sz = baseZ + level * 2;
            int halfX = sx / 2;
            int halfZ = sz / 2;
            int x = loc.getBlockX();
            int z = loc.getBlockZ();
            if (x < cx - halfX || x >= cx - halfX + sx || z < cz - halfZ || z >= cz - halfZ + sz) continue;
            return n;
        }
        return null;
    }

    public boolean canInteract(Player player, Location loc) {
        if (player == null || loc == null) {
            return false;
        }
        if (this.isInSpawnProtection(loc)) {
            return true;
        }
        Nation owner = this.getTerritoryOwner(loc);
        if (owner == null) {
            return true;
        }
        Nation playerNation = this.nationService.getNationOf(player.getUniqueId());
        return playerNation != null && playerNation.getName().equalsIgnoreCase(owner.getName());
    }

    public boolean canModify(Player player, Block block) {
        if (block == null) {
            return true;
        }
        return this.canInteract(player, block.getLocation());
    }
}

