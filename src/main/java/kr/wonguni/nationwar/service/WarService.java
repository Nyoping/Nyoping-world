/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.WorldCreator
 *  org.bukkit.WorldType
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Slime
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.service.MoneyService;
import kr.wonguni.nationwar.service.NationService;
import kr.wonguni.nationwar.service.ProtectionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class WarService {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final NationService nationService;
    private final MoneyService moneyService;
    private final ProtectionService protectionService;
    private World warWorld;
    private WarMatch currentMatch;
    private int matchTickTask = -1;

    public WarService(JavaPlugin plugin, DataStore store, NationService nationService, MoneyService moneyService, ProtectionService protectionService) {
        this.plugin = plugin;
        this.store = store;
        this.nationService = nationService;
        this.moneyService = moneyService;
        this.protectionService = protectionService;
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public void start() {
        this.ensureWarWorld();
        this.matchTickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (this.matchTickTask != -1) {
            Bukkit.getScheduler().cancelTask(this.matchTickTask);
        }
    }

    public void ensureWarWorld() {
        String name = this.plugin.getConfig().getString("war.world-name", "War_World");
        this.warWorld = Bukkit.getWorld((String)name);
        if (this.warWorld == null) {
            WorldCreator wc = new WorldCreator(name);
            wc.environment(World.Environment.NORMAL);
            wc.type(WorldType.FLAT);
            wc.generateStructures(false);
            this.warWorld = Bukkit.createWorld((WorldCreator)wc);
        }
        if (this.warWorld != null) {
            double borderSize = this.plugin.getConfig().getDouble("war.border-size", 500.0);
            this.warWorld.getWorldBorder().setCenter(0.0, 0.0);
            this.warWorld.getWorldBorder().setSize(borderSize);
        }
    }

    public World getWarWorld() {
        return this.warWorld;
    }

    public boolean isWarWorld(World w) {
        return this.warWorld != null && w != null && this.warWorld.getName().equalsIgnoreCase(w.getName());
    }

    public boolean startChallenge(Player challenger, Nation targetNation) {
        Nation a = this.nationService.getNationOf(challenger.getUniqueId());
        if (a == null) {
            challenger.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (targetNation == null) {
            challenger.sendMessage("\u00a7c\ub300\uc0c1 \uad6d\uac00\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (a.getName().equalsIgnoreCase(targetNation.getName())) {
            challenger.sendMessage("\u00a7c\uc790\uae30 \uad6d\uac00\uc640\ub294 \uc804\uc7c1\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (this.currentMatch != null && !this.currentMatch.isFinished()) {
            challenger.sendMessage("\u00a7c\uc774\ubbf8 \uc9c4\ud589 \uc911\uc778 \uc804\uc7c1 \ub9e4\uce6d\uc774 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        int min = this.plugin.getConfig().getInt("war.match.min-team-size", 3);
        int ready = this.plugin.getConfig().getInt("war.match.ready-time-seconds", 120);
        this.currentMatch = new WarMatch(a.getName(), targetNation.getName(), min, ready);
        this.broadcastToNation(a, "\u00a7e[\uc804\uc7c1] \u00a7f" + targetNation.getName() + " \uad6d\uac00\uc640 \uc804\uc7c1 \ub9e4\uce6d\uc744 \uc2dc\uc791\ud569\ub2c8\ub2e4. \uc218\ub77d: \u00a7a/war accept \u00a7f\uac70\uc808: \u00a7c/war decline");
        this.broadcastToNation(targetNation, "\u00a7e[\uc804\uc7c1] \u00a7f" + a.getName() + " \uad6d\uac00\uac00 \uc804\uc7c1 \ub9e4\uce6d\uc744 \uc2dc\uc791\ud588\uc2b5\ub2c8\ub2e4. \uc218\ub77d: \u00a7a/war accept \u00a7f\uac70\uc808: \u00a7c/war decline");
        return true;
    }

    public boolean forceStart(String nationAName, String nationBName) {
        Player p;
        this.ensureWarWorld();
        Nation na = this.nationService.getNationByName(nationAName);
        Nation nb = this.nationService.getNationByName(nationBName);
        if (na == null || nb == null) {
            return false;
        }
        if (this.currentMatch != null && !this.currentMatch.isFinished()) {
            this.currentMatch.finish();
        }
        this.currentMatch = new WarMatch(na.getName(), nb.getName(), 0, 0);
        for (UUID u : na.getMembers()) {
            p = Bukkit.getPlayer((UUID)u);
            if (p == null || !p.isOnline()) continue;
            this.currentMatch.teamA.add(u);
        }
        for (UUID u : nb.getMembers()) {
            p = Bukkit.getPlayer((UUID)u);
            if (p == null || !p.isOnline()) continue;
            this.currentMatch.teamB.add(u);
        }
        this.startWar();
        return true;
    }

    public void accept(Player player) {
        if (this.currentMatch == null) {
            player.sendMessage("\u00a7c\uc9c4\ud589 \uc911\uc778 \ub9e4\uce6d\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Nation n = this.nationService.getNationOf(player.getUniqueId());
        if (n == null) {
            player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        if (!this.currentMatch.isParticipantNation(n.getName())) {
            player.sendMessage("\u00a7c\ub2f9\uc2e0 \uad6d\uac00\ub294 \uc774 \ub9e4\uce6d\uc5d0 \uc18d\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
            return;
        }
        this.currentMatch.accept(player.getUniqueId(), n.getName());
        player.sendMessage("\u00a7a\uc804\uc7c1 \ub9e4\uce6d \ucc38\uac00\ub97c \uc218\ub77d\ud588\uc2b5\ub2c8\ub2e4.");
    }

    public void decline(Player player) {
        if (this.currentMatch == null) {
            player.sendMessage("\u00a7c\uc9c4\ud589 \uc911\uc778 \ub9e4\uce6d\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Nation n = this.nationService.getNationOf(player.getUniqueId());
        if (n == null) {
            player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        if (!this.currentMatch.isParticipantNation(n.getName())) {
            player.sendMessage("\u00a7c\ub2f9\uc2e0 \uad6d\uac00\ub294 \uc774 \ub9e4\uce6d\uc5d0 \uc18d\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
            return;
        }
        this.currentMatch.decline(player.getUniqueId(), n.getName());
        player.sendMessage("\u00a7e\uc804\uc7c1 \ub9e4\uce6d \ucc38\uac00\ub97c \uac70\uc808\ud588\uc2b5\ub2c8\ub2e4.");
    }

    public void status(Player player) {
        if (this.currentMatch == null) {
            player.sendMessage("\u00a7c\uc9c4\ud589 \uc911\uc778 \ub9e4\uce6d\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        player.sendMessage("\u00a7e[\uc804\uc7c1] \u00a7f" + this.currentMatch.nationA + " vs " + this.currentMatch.nationB + " (\ub0a8\uc740\uc2dc\uac04 " + this.currentMatch.secondsLeft + "\ucd08)");
        player.sendMessage("\u00a77\ucc38\uac00: " + this.currentMatch.teamA.size() + " vs " + this.currentMatch.teamB.size() + "  (\ucd5c\uc18c " + this.currentMatch.minTeam + " vs " + this.currentMatch.minTeam + ")");
    }

    private void tick() {
        if (this.currentMatch == null) {
            return;
        }
        if (this.currentMatch.isFinished()) {
            this.currentMatch = null;
            return;
        }
        --this.currentMatch.secondsLeft;
        if (this.currentMatch.secondsLeft % 30 == 0 || this.currentMatch.secondsLeft <= 10) {
            Nation na = this.nationService.getNationByName(this.currentMatch.nationA);
            Nation nb = this.nationService.getNationByName(this.currentMatch.nationB);
            if (na != null) {
                this.broadcastToNation(na, "\u00a7e[\uc804\uc7c1] \u00a7f\ub9e4\uce6d \ub300\uae30: " + this.currentMatch.teamA.size() + " vs " + this.currentMatch.teamB.size() + " (" + this.currentMatch.secondsLeft + "\ucd08)");
            }
            if (nb != null) {
                this.broadcastToNation(nb, "\u00a7e[\uc804\uc7c1] \u00a7f\ub9e4\uce6d \ub300\uae30: " + this.currentMatch.teamA.size() + " vs " + this.currentMatch.teamB.size() + " (" + this.currentMatch.secondsLeft + "\ucd08)");
            }
        }
        if (this.currentMatch.secondsLeft <= 0) {
            this.cancelMatch("\ub300\uae30\uc2dc\uac04 \ucd08\uacfc");
            return;
        }
        if (this.currentMatch.teamA.size() >= this.currentMatch.minTeam && this.currentMatch.teamB.size() >= this.currentMatch.minTeam && this.currentMatch.teamA.size() == this.currentMatch.teamB.size()) {
            this.startWar();
        }
    }

    private void cancelMatch(String reason) {
        Nation na = this.nationService.getNationByName(this.currentMatch.nationA);
        Nation nb = this.nationService.getNationByName(this.currentMatch.nationB);
        if (na != null) {
            this.broadcastToNation(na, "\u00a7c[\uc804\uc7c1] \ub9e4\uce6d \ucde8\uc18c: " + reason);
        }
        if (nb != null) {
            this.broadcastToNation(nb, "\u00a7c[\uc804\uc7c1] \ub9e4\uce6d \ucde8\uc18c: " + reason);
        }
        this.currentMatch.finish();
    }

    private void startWar() {
        Player p;
        this.ensureWarWorld();
        if (this.warWorld == null) {
            this.cancelMatch("War_World \uc0dd\uc131 \uc2e4\ud328");
            return;
        }
        Nation na = this.nationService.getNationByName(this.currentMatch.nationA);
        Nation nb = this.nationService.getNationByName(this.currentMatch.nationB);
        if (na == null || nb == null) {
            this.cancelMatch("\uad6d\uac00 \uc815\ubcf4 \uc5c6\uc74c");
            return;
        }
        Location aSpawn = this.loadSpawn("war.spawns.team-a", this.warWorld);
        Location bSpawn = this.loadSpawn("war.spawns.team-b", this.warWorld);
        for (UUID u : this.currentMatch.teamA) {
            p = Bukkit.getPlayer((UUID)u);
            if (p == null) continue;
            p.teleport(aSpawn);
        }
        for (UUID u : this.currentMatch.teamB) {
            p = Bukkit.getPlayer((UUID)u);
            if (p == null) continue;
            p.teleport(bSpawn);
        }
        this.spawnTeamStructures(aSpawn, na, true);
        this.spawnTeamStructures(bSpawn, nb, false);
        this.broadcastToNation(na, "\u00a7a[\uc804\uc7c1] \uc804\uc7c1\uc774 \uc2dc\uc791\ub418\uc5c8\uc2b5\ub2c8\ub2e4!");
        this.broadcastToNation(nb, "\u00a7a[\uc804\uc7c1] \uc804\uc7c1\uc774 \uc2dc\uc791\ub418\uc5c8\uc2b5\ub2c8\ub2e4!");
        this.currentMatch.finish();
    }

    private Location loadSpawn(String path, World world) {
        double x = this.plugin.getConfig().getDouble(path + ".x");
        double y = this.plugin.getConfig().getDouble(path + ".y");
        double z = this.plugin.getConfig().getDouble(path + ".z");
        return new Location(world, x, y, z);
    }

    private void spawnTeamStructures(Location center, Nation nation, boolean negativeSide) {
        Random r = new Random();
        int ox = r.nextInt(41) - 20;
        int oz = r.nextInt(41) - 20;
        Location base = center.clone().add((double)ox, 0.0, (double)oz);
        base.setY((double)(center.getWorld().getHighestBlockYAt(base) + 1));
        this.spawnStructure(base.clone(), "NEXUS", nation);
        this.spawnStructure(base.clone().add(10.0, 0.0, 0.0), "INHIBITOR", nation);
        this.spawnStructure(base.clone().add(-10.0, 0.0, 0.0), "TURRET", nation);
    }

    public void spawnPlacedNexus(Location loc, Nation nation) {
        this.spawnStructure(loc, "NEXUS", nation);
    }

    public void checkAndRepairNexus(Nation nation) {
        if (nation == null || !nation.hasNexus()) {
            return;
        }
        World w = Bukkit.getWorld((String)nation.getNexusWorld());
        if (w == null) {
            return;
        }
        Location loc = new Location(w, (double)nation.getNexusX() + 0.5, (double)nation.getNexusY() + 0.5, (double)nation.getNexusZ() + 0.5);
        boolean found = false;
        for (Entity ent : w.getNearbyEntities(loc, 2.0, 2.0, 2.0)) {
            if (!(ent instanceof Slime) || !ent.getScoreboardTags().contains("NW_STRUCTURE") || !ent.getScoreboardTags().contains("NW_TYPE_NEXUS") || !ent.getScoreboardTags().contains("NW_NATION_" + nation.getName())) continue;
            found = true;
            break;
        }
        if (!found) {
            this.plugin.getLogger().info("Repairing missing nexus for nation: " + nation.getName());
            this.spawnPlacedNexus(loc, nation);
        }
    }

    public void removePlacedNexus(Nation nation) {
        if (nation == null || !nation.hasNexus()) {
            return;
        }
        String worldName = nation.getNexusWorld();
        World w = Bukkit.getWorld((String)worldName);
        if (w == null) {
            return;
        }
        for (Entity ent : w.getEntities()) {
            Slime slime;
            if (!(ent instanceof Slime) || !(slime = (Slime)ent).getScoreboardTags().contains("NW_STRUCTURE") || !slime.getScoreboardTags().contains("NW_TYPE_NEXUS") || !slime.getScoreboardTags().contains("NW_NATION_" + nation.getName())) continue;
            slime.remove();
        }
    }

    private void spawnStructure(Location loc, String type, Nation nation) {
        Slime slime = (Slime)loc.getWorld().spawnEntity(loc, EntityType.SLIME);
        slime.setAI(false);
        slime.setInvulnerable(false);
        slime.setSilent(true);
        slime.setCollidable(false);
        slime.setGravity(false);
        slime.setPersistent(true);
        slime.setRemoveWhenFarAway(false);
        if ("NEXUS".equals(type) || "INHIBITOR".equals(type)) {
            slime.setSize(8);
        } else {
            slime.setSize(4);
        }
        slime.addScoreboardTag("NW_STRUCTURE");
        slime.addScoreboardTag("NW_TYPE_" + type);
        slime.addScoreboardTag("NW_NATION_" + nation.getName());
        slime.setCustomNameVisible(true);
        slime.setCustomName("\u00a7b" + nation.getName() + " " + type);
        double baseMax = switch (type) {
            case "NEXUS" -> this.plugin.getConfig().getDouble("structures.nexus.max-health", 50.0);
            case "INHIBITOR" -> this.plugin.getConfig().getDouble("structures.inhibitor.max-health", 20.0);
            case "TURRET" -> this.plugin.getConfig().getDouble("structures.turret.max-health", 10.0);
            default -> 10.0;
        };
        int lvl = switch (type) {
            case "TURRET" -> nation.getTurretLevel();
            case "INHIBITOR" -> nation.getInhibitorLevel();
            default -> 0;
        };
        double scale = 1.0 + 0.1 * (double)Math.max(0, lvl);
        double max = baseMax * scale;
        slime.getPersistentDataContainer().set(new NamespacedKey((Plugin)this.plugin, "nw_hp"), PersistentDataType.DOUBLE, (Double)max);
        slime.getPersistentDataContainer().set(new NamespacedKey((Plugin)this.plugin, "nw_hpmax"), PersistentDataType.DOUBLE, (Double)max);
    }

    private void broadcastToNation(Nation n, String msg) {
        for (UUID u : n.getMembers()) {
            Player p = Bukkit.getPlayer((UUID)u);
            if (p == null) continue;
            p.sendMessage(msg);
        }
    }

    public void cancelDamageIfOverworldNexus(Entity entity, EntityDamageEvent event) {
        if (!(entity instanceof Slime)) {
            return;
        }
        Slime slime = (Slime)entity;
        if (!slime.getScoreboardTags().contains("NW_STRUCTURE")) {
            return;
        }
        if (this.warWorld == null) {
            return;
        }
        if (!this.isWarWorld(entity.getWorld()) && this.plugin.getConfig().getBoolean("nexus.overworld-invulnerable", true)) {
            event.setCancelled(true);
        }
    }

    public void damageStructure(Slime slime, double amount, Player damager) {
        if (!this.isWarWorld(slime.getWorld())) {
            return;
        }
        if (!slime.getScoreboardTags().contains("NW_STRUCTURE")) {
            return;
        }
        NamespacedKey hpKey = new NamespacedKey((Plugin)this.plugin, "nw_hp");
        NamespacedKey hpMaxKey = new NamespacedKey((Plugin)this.plugin, "nw_hpmax");
        Double hp = (Double)slime.getPersistentDataContainer().get(hpKey, PersistentDataType.DOUBLE);
        Double mx = (Double)slime.getPersistentDataContainer().get(hpMaxKey, PersistentDataType.DOUBLE);
        if (hp == null || mx == null) {
            return;
        }
        double newHp = Math.max(0.0, hp - amount);
        slime.getPersistentDataContainer().set(hpKey, PersistentDataType.DOUBLE, (Double)newHp);
        slime.setCustomName("\u00a7b" + this.getNationTag(slime) + " " + this.getTypeTag(slime) + " \u00a7f(" + String.format(Locale.US, "%.1f", newHp) + "/" + String.format(Locale.US, "%.1f", mx) + ")");
        if (newHp <= 0.0) {
            slime.remove();
            if (damager != null) {
                damager.sendMessage("\u00a7a\uad6c\uc870\ubb3c\uc744 \ud30c\uad34\ud588\uc2b5\ub2c8\ub2e4: " + this.getTypeTag(slime));
            }
        }
    }

    private String getTypeTag(Slime slime) {
        for (String t : slime.getScoreboardTags()) {
            if (!t.startsWith("NW_TYPE_")) continue;
            return t.substring("NW_TYPE_".length());
        }
        return "STRUCTURE";
    }

    private String getNationTag(Slime slime) {
        for (String t : slime.getScoreboardTags()) {
            if (!t.startsWith("NW_NATION_")) continue;
            return t.substring("NW_NATION_".length());
        }
        return "?";
    }

    public int removeNationStructures(String nationName) {
        if (nationName == null || nationName.isBlank()) {
            return 0;
        }
        String tag = "NW_NATION_" + nationName;
        int removed = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Entity ent : w.getEntities()) {
                Slime slime;
                if (!(ent instanceof Slime) || !(slime = (Slime)ent).getScoreboardTags().contains("NW_STRUCTURE")) continue;
                boolean sameNation = false;
                for (String t : slime.getScoreboardTags()) {
                    if (!t.equalsIgnoreCase(tag)) continue;
                    sameNation = true;
                    break;
                }
                if (!sameNation) continue;
                slime.remove();
                ++removed;
            }
        }
        return removed;
    }

    public DataStore getStore() {
        return this.store;
    }

    private static class WarMatch {
        final String nationA;
        final String nationB;
        final int minTeam;
        int secondsLeft;
        final Set<UUID> teamA = new HashSet<UUID>();
        final Set<UUID> teamB = new HashSet<UUID>();
        boolean finished = false;

        WarMatch(String a, String b, int minTeam, int readySeconds) {
            this.nationA = a;
            this.nationB = b;
            this.minTeam = minTeam;
            this.secondsLeft = readySeconds;
        }

        boolean isParticipantNation(String name) {
            return name != null && (this.nationA.equalsIgnoreCase(name) || this.nationB.equalsIgnoreCase(name));
        }

        void accept(UUID uuid, String nationName) {
            if (uuid == null || nationName == null) {
                return;
            }
            if (this.nationA.equalsIgnoreCase(nationName)) {
                this.teamA.add(uuid);
                this.teamB.remove(uuid);
            } else if (this.nationB.equalsIgnoreCase(nationName)) {
                this.teamB.add(uuid);
                this.teamA.remove(uuid);
            }
        }

        void decline(UUID uuid, String nationName) {
            if (uuid == null || nationName == null) {
                return;
            }
            if (this.nationA.equalsIgnoreCase(nationName)) {
                this.teamA.remove(uuid);
            }
            if (this.nationB.equalsIgnoreCase(nationName)) {
                this.teamB.remove(uuid);
            }
        }

        boolean isFinished() {
            return this.finished;
        }

        void finish() {
            this.finished = true;
        }
    }
}

