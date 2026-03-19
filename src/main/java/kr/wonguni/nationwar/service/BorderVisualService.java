/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package kr.wonguni.nationwar.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.service.NationService;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BorderVisualService {
    private final JavaPlugin plugin;
    private final NationService nationService;
    private final Map<UUID, Integer> activeTasks = new ConcurrentHashMap<UUID, Integer>();

    public BorderVisualService(JavaPlugin plugin, NationService nationService) {
        this.plugin = plugin;
        this.nationService = nationService;
        int period = Math.max(2, plugin.getConfig().getInt("ui.border-visual.overlay-period-ticks", 10));
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> {
            try {
                this.tickOverlay();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }, 40L, (long)period);
    }

    public void stopAll() {
        for (Integer id : this.activeTasks.values()) {
            try {
                this.plugin.getServer().getScheduler().cancelTask(id.intValue());
            }
            catch (Exception exception) {}
        }
        this.activeTasks.clear();
    }

    public void showOwnNationBorder(Player player, int seconds) {
        Nation n = this.nationService.getNationOf(player.getUniqueId());
        if (n == null || !n.hasNexus()) {
            player.sendMessage("\u00a7c\uad6d\uac00 \ub125\uc11c\uc2a4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        this.showNationBorder(player, n, seconds);
    }

    public void showNationBorder(final Player player, final Nation nation, int seconds) {
        if (player == null || nation == null || !nation.hasNexus()) {
            return;
        }
        Integer prev = this.activeTasks.remove(player.getUniqueId());
        if (prev != null) {
            try {
                this.plugin.getServer().getScheduler().cancelTask(prev.intValue());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        int baseX = this.plugin.getConfig().getInt("nation.territory.size-x", 16);
        int baseZ = this.plugin.getConfig().getInt("nation.territory.size-z", 16);
        int level = Math.max(0, nation.getLevel());
        int sx = baseX + level * 2;
        int sz = baseZ + level * 2;
        int halfX = sx / 2;
        int halfZ = sz / 2;
        final int minX = nation.getNexusX() - halfX;
        final int minZ = nation.getNexusZ() - halfZ;
        final int maxX = minX + sx;
        final int maxZ = minZ + sz;
        final int tickPeriod = Math.max(2, this.plugin.getConfig().getInt("ui.border-visual.period-ticks", 5));
        final int totalTicks = Math.max(20, seconds * 20);
        final Particle particle = Particle.valueOf((String)this.plugin.getConfig().getString("ui.border-visual.particle", "DUST"));
        float size = (float)this.plugin.getConfig().getDouble("ui.border-visual.dust-size", 1.1);
        final Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB((int)0, (int)255, (int)255), size);
        BukkitRunnable task = new BukkitRunnable(){
            int lived = 0;

            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if (player.getWorld() == null) {
                    this.cancel();
                    return;
                }
                if (!player.getWorld().getName().equalsIgnoreCase(nation.getNexusWorld())) {
                    this.lived += tickPeriod;
                    if (this.lived >= totalTicks) {
                        this.cancel();
                    }
                    return;
                }
                double y = player.getLocation().getY() + 0.2;
                double step = BorderVisualService.this.plugin.getConfig().getDouble("ui.border-visual.step", 1.0);
                for (double x = (double)minX; x <= (double)maxX; x += step) {
                    this.spawn(player, particle, new Location(player.getWorld(), x, y, (double)minZ), dust);
                    this.spawn(player, particle, new Location(player.getWorld(), x, y, (double)maxZ), dust);
                }
                for (double z = (double)minZ; z <= (double)maxZ; z += step) {
                    this.spawn(player, particle, new Location(player.getWorld(), (double)minX, y, z), dust);
                    this.spawn(player, particle, new Location(player.getWorld(), (double)maxX, y, z), dust);
                }
                this.lived += tickPeriod;
                if (this.lived >= totalTicks) {
                    this.cancel();
                }
            }

            private void spawn(Player p, Particle particle2, Location loc, Particle.DustOptions dust2) {
                try {
                    if (particle2 == Particle.DUST) {
                        p.spawnParticle(particle2, loc, 1, 0.0, 0.0, 0.0, 0.0, (Object)dust2);
                    } else {
                        p.spawnParticle(particle2, loc, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                catch (IllegalArgumentException ex) {
                    p.spawnParticle(Particle.END_ROD, loc, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }

            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                BorderVisualService.this.activeTasks.remove(player.getUniqueId());
            }
        };
        int id = task.runTaskTimer((Plugin)this.plugin, 0L, (long)tickPeriod).getTaskId();
        this.activeTasks.put(player.getUniqueId(), id);
        player.sendMessage("\u00a7b[\ubcf4\ud638\uad6c\uc5ed] \u00a7f\uacbd\uacc4 \ud45c\uc2dc\ub97c " + seconds + "\ucd08 \ub3d9\uc548 \ubcf4\uc5ec\uc90d\ub2c8\ub2e4.");
    }

    private void tickOverlay() {
        Particle particle = Particle.valueOf((String)this.plugin.getConfig().getString("ui.border-visual.particle", "DUST"));
        float size = (float)this.plugin.getConfig().getDouble("ui.border-visual.dust-size", 1.0);
        Particle.DustOptions dustOwn = new Particle.DustOptions(Color.fromRGB((int)0, (int)255, (int)255), size);
        Particle.DustOptions dustOther = new Particle.DustOptions(Color.fromRGB((int)255, (int)255, (int)0), size);
        double step = this.plugin.getConfig().getDouble("ui.border-visual.step", 2.0);
        int baseX = this.plugin.getConfig().getInt("nation.territory.size-x", 16);
        int baseZ = this.plugin.getConfig().getInt("nation.territory.size-z", 16);
        double otherRange = this.plugin.getConfig().getDouble("ui.border-visual.other-max-distance", 128.0);
        double otherRangeSq = otherRange * otherRange;
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            PlayerProfile prof = this.nationService.getStore().getOrCreatePlayer(p.getUniqueId());
            boolean ownOn = prof.isShowOwnBorders();
            boolean otherOn = prof.isShowOtherBorders();
            if (!ownOn && !otherOn) continue;
            Nation own = this.nationService.getNationOf(p.getUniqueId());
            Location ploc = p.getLocation();
            double y = ploc.getY() + 0.2;
            if (ownOn && own != null && own.hasNexus() && ploc.getWorld() != null && ploc.getWorld().getName().equalsIgnoreCase(own.getNexusWorld())) {
                this.drawBorderOnce(p, own, baseX, baseZ, step, y, particle, dustOwn);
            }
            if (!otherOn) continue;
            for (Nation n : this.nationService.getAllNations()) {
                double dz;
                double dx;
                if (n == null || !n.hasNexus() || own != null && own.getName() != null && n.getName() != null && own.getName().equalsIgnoreCase(n.getName()) || ploc.getWorld() == null || !ploc.getWorld().getName().equalsIgnoreCase(n.getNexusWorld()) || (dx = ploc.getX() - (double)n.getNexusX()) * dx + (dz = ploc.getZ() - (double)n.getNexusZ()) * dz > otherRangeSq) continue;
                this.drawBorderOnce(p, n, baseX, baseZ, step, y, particle, dustOther);
            }
        }
    }

    private void drawBorderOnce(Player viewer, Nation nation, int baseX, int baseZ, double step, double y, Particle particle, Particle.DustOptions dust) {
        int level = Math.max(0, nation.getLevel());
        int sx = baseX + level * 2;
        int sz = baseZ + level * 2;
        int halfX = sx / 2;
        int halfZ = sz / 2;
        int minX = nation.getNexusX() - halfX;
        int minZ = nation.getNexusZ() - halfZ;
        int maxX = minX + sx;
        int maxZ = minZ + sz;
        for (double x = (double)minX; x <= (double)maxX; x += step) {
            this.spawnOnce(viewer, particle, new Location(viewer.getWorld(), x, y, (double)minZ), dust);
            this.spawnOnce(viewer, particle, new Location(viewer.getWorld(), x, y, (double)maxZ), dust);
        }
        for (double z = (double)minZ; z <= (double)maxZ; z += step) {
            this.spawnOnce(viewer, particle, new Location(viewer.getWorld(), (double)minX, y, z), dust);
            this.spawnOnce(viewer, particle, new Location(viewer.getWorld(), (double)maxX, y, z), dust);
        }
    }

    private void spawnOnce(Player p, Particle particle, Location loc, Particle.DustOptions dust) {
        try {
            if (particle == Particle.DUST) {
                p.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0, (Object)dust);
            } else {
                p.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

