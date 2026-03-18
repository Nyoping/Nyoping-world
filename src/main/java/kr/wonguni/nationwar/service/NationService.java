/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.service.MoneyService;
import kr.wonguni.nationwar.util.Items;
import kr.wonguni.nationwar.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class NationService {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final MoneyService moneyService;
    private final Map<UUID, String> pendingInvites = new HashMap<UUID, String>();
    private int upkeepTaskId = -1;

    public NationService(JavaPlugin plugin, DataStore store, MoneyService moneyService) {
        this.plugin = plugin;
        this.store = store;
        this.moneyService = moneyService;
    }

    public DataStore getStore() {
        return this.store;
    }

    public boolean territoryOverlapsOtherNations(Nation self, String worldName, int cx, int cz, int level) {
        if (worldName == null) {
            return false;
        }
        int baseX = this.plugin.getConfig().getInt("nation.territory.size-x", 16);
        int baseZ = this.plugin.getConfig().getInt("nation.territory.size-z", 16);
        int lv = Math.max(0, level);
        int sx = baseX + lv * 2;
        int sz = baseZ + lv * 2;
        int halfX = sx / 2;
        int halfZ = sz / 2;
        int minAx = cx - halfX;
        int maxAx = minAx + sx;
        int minAz = cz - halfZ;
        int maxAz = minAz + sz;
        for (Nation other : this.store.getAllNations()) {
            boolean overlapZ;
            if (other == null || self != null && other.getName().equalsIgnoreCase(self.getName()) || !other.hasNexus() || other.getNexusWorld() == null || !other.getNexusWorld().equalsIgnoreCase(worldName)) continue;
            int ocx = other.getNexusX();
            int ocz = other.getNexusZ();
            int olv = Math.max(0, other.getLevel());
            int osx = baseX + olv * 2;
            int osz = baseZ + olv * 2;
            int ohalfX = osx / 2;
            int ohalfZ = osz / 2;
            int minBx = ocx - ohalfX;
            int maxBx = minBx + osx;
            int minBz = ocz - ohalfZ;
            int maxBz = minBz + osz;
            boolean overlapX = maxAx > minBx && maxBx > minAx;
            boolean bl = overlapZ = maxAz > minBz && maxBz > minAz;
            if (!overlapX || !overlapZ) continue;
            return true;
        }
        return false;
    }

    public void startDailyUpkeepScheduler() {
        long delayTicks = Math.max(20L, TimeUtil.millisUntil(TimeUtil.nextKstMidnight()) / 50L);
        this.upkeepTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, this::runDailyUpkeep, delayTicks, 1728000L);
    }

    public void stop() {
        if (this.upkeepTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.upkeepTaskId);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Nation getNationOf(UUID uuid) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            return this.store.getNationOf(uuid);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Nation getNationByName(String name) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            return this.store.getNationByName(name);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Collection<Nation> getAllNations() {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            return this.store.getAllNations();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean createNation(Player creator, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.length() > 16) {
            creator.sendMessage("\u00a7c\uad6d\uac00 \uc774\ub984\uc740 16\uc790 \uc774\ud558\uc5ec\uc57c \ud569\ub2c8\ub2e4.");
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            if (this.store.getNationOf(creator.getUniqueId()) != null) {
                creator.sendMessage("\u00a7c\uc774\ubbf8 \uad6d\uac00\uc5d0 \uc18c\uc18d\ub418\uc5b4 \uc788\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (this.store.getNationByName(name) != null) {
                creator.sendMessage("\u00a7c\uc774\ubbf8 \uc874\uc7ac\ud558\ub294 \uad6d\uac00 \uc774\ub984\uc785\ub2c8\ub2e4.");
                return false;
            }
            Nation n = new Nation(name, creator.getUniqueId());
            this.store.putNation(n);
            this.store.setNationOf(creator.getUniqueId(), n);
            this.store.saveAll();
        }
        creator.sendMessage("\u00a7a\uad6d\uac00 '" + name + "' \uc0dd\uc131 \uc644\ub8cc!");
        return true;
    }

    public boolean invite(Player inviter, OfflinePlayer target) {
        if (target == null) {
            return false;
        }
        Nation n = this.getNationOf(inviter.getUniqueId());
        if (n == null) {
            inviter.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!n.isOfficer(inviter.getUniqueId())) {
            inviter.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \ucd08\ub300\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (this.getNationOf(target.getUniqueId()) != null) {
            inviter.sendMessage("\u00a7c\ub300\uc0c1\uc740 \uc774\ubbf8 \ub2e4\ub978 \uad6d\uac00\uc5d0 \uc18c\uc18d\ub418\uc5b4 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        this.pendingInvites.put(target.getUniqueId(), n.getName().toLowerCase(Locale.ROOT));
        inviter.sendMessage("\u00a7e" + target.getName() + "\u00a7f \ub2d8\uc744 \uad6d\uac00\uc5d0 \ucd08\ub300\ud588\uc2b5\ub2c8\ub2e4.");
        if (target.isOnline()) {
            Player p = target.getPlayer();
            p.sendMessage("\u00a7e[\uad6d\uac00] \u00a7f'" + n.getName() + "' \uad6d\uac00\uc5d0 \ucd08\ub300\ub418\uc5c8\uc2b5\ub2c8\ub2e4. \u00a7a/nation accept \u00a7f\ub610\ub294 \u00a7c/nation decline");
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean acceptInvite(Player player) {
        String nl = this.pendingInvites.get(player.getUniqueId());
        if (nl == null) {
            player.sendMessage("\u00a7c\ubc1b\uc740 \ucd08\ub300\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n;
            Nation cur = this.store.getNationOf(player.getUniqueId());
            if (cur != null) {
                if (player.getUniqueId().equals(cur.getLeaderUuid())) {
                    this.store.removeNation(cur.getName());
                    player.sendMessage("\u00a7e[\uad6d\uac00] \uae30\uc874 \uad6d\uac00\ub97c \ucd08\uae30\ud654\ud558\uace0 \ucd08\ub300\ub97c \uc218\ub77d\ud569\ub2c8\ub2e4.");
                } else {
                    cur.removeMember(player.getUniqueId());
                    this.store.setNationOf(player.getUniqueId(), null);
                    player.sendMessage("\u00a7e[\uad6d\uac00] \uae30\uc874 \uad6d\uac00\uc5d0\uc11c \ud0c8\ud1f4\ud558\uace0 \ucd08\ub300\ub97c \uc218\ub77d\ud569\ub2c8\ub2e4.");
                }
                PlayerProfile prof = this.store.getOrCreatePlayer(player.getUniqueId());
                prof.setPendingNexusOnLeave(true);
                try {
                    PlayerInventory inv = player.getInventory();
                    for (int i = 0; i < inv.getSize(); ++i) {
                        ItemStack it = inv.getItem(i);
                        if (!Items.isNexusItem(this.plugin, it)) continue;
                        inv.setItem(i, null);
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if ((n = this.store.getNationByName(nl)) == null) {
                player.sendMessage("\u00a7c\ud574\ub2f9 \uad6d\uac00\ub294 \ub354 \uc774\uc0c1 \uc874\uc7ac\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
                this.pendingInvites.remove(player.getUniqueId());
                return false;
            }
            n.addMember(player.getUniqueId());
            this.store.setNationOf(player.getUniqueId(), n);
            this.pendingInvites.remove(player.getUniqueId());
            this.store.saveAll();
        }
        player.sendMessage("\u00a7a\uad6d\uac00 \uac00\uc785 \uc644\ub8cc!");
        return true;
    }

    public boolean declineInvite(Player player) {
        if (this.pendingInvites.remove(player.getUniqueId()) != null) {
            player.sendMessage("\u00a7e\uad6d\uac00 \ucd08\ub300\ub97c \uac70\uc808\ud588\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        player.sendMessage("\u00a7c\ubc1b\uc740 \ucd08\ub300\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean leaveNation(Player player) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationOf(player.getUniqueId());
            if (n == null) {
                player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (player.getUniqueId().equals(n.getLeaderUuid())) {
                player.sendMessage("\u00a7c\uad6d\uac00\uc7a5\uc740 \ud0c8\ud1f4\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4. /nation disband \ub610\ub294 \uad6d\uac00\uc7a5 \uc704\uc784 \ud544\uc694(\ucd94\ud6c4).");
                return false;
            }
            n.removeMember(player.getUniqueId());
            this.store.setNationOf(player.getUniqueId(), null);
            this.store.saveAll();
        }
        player.sendMessage("\u00a7e\uad6d\uac00\uc5d0\uc11c \ud0c8\ud1f4\ud588\uc2b5\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean disband(Player player) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationOf(player.getUniqueId());
            if (n == null) {
                player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!player.getUniqueId().equals(n.getLeaderUuid())) {
                player.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \ud574\uccb4\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            for (UUID u : new ArrayList<UUID>(n.getMembers())) {
                this.store.setNationOf(u, null);
            }
            this.store.removeNation(n.getName());
            this.store.saveAll();
        }
        player.sendMessage("\u00a7c\uad6d\uac00\ub97c \ud574\uccb4\ud588\uc2b5\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean setRelocationMode(Player leader, boolean enabled) {
        if (leader == null) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationOf(leader.getUniqueId());
            if (n == null) {
                leader.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!leader.getUniqueId().equals(n.getLeaderUuid())) {
                leader.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                return false;
            }
            n.setRelocationMode(enabled);
            this.store.saveAll();
        }
        leader.sendMessage(enabled ? "\u00a7e\ub125\uc11c\uc2a4 \uc62e\uae30\uae30 \ubaa8\ub4dc: \u00a7aON" : "\u00a7e\ub125\uc11c\uc2a4 \uc62e\uae30\uae30 \ubaa8\ub4dc: \u00a7cOFF");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean collapse(Player player) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationOf(player.getUniqueId());
            if (n == null) {
                player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!player.getUniqueId().equals(n.getLeaderUuid())) {
                player.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uba78\ub9dd\uc2dc\ud0ac \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (n.getMembers().size() > 2) {
                player.sendMessage("\u00a7c\uad6d\uac00\uc6d0\uc774 2\uba85 \uc774\ud558\uc77c \ub54c\ub9cc \uba78\ub9dd\uc774 \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                return false;
            }
            for (UUID u : new ArrayList<UUID>(n.getMembers())) {
                this.store.setNationOf(u, null);
            }
            this.store.removeNation(n.getName());
            this.store.saveAll();
        }
        player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uba78\ub9dd\ud588\uc2b5\ub2c8\ub2e4. \uc774\uc81c \ubb34\uc18c\uc18d\uc785\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean transferLeader(Player leader, UUID newLeader) {
        if (leader == null || newLeader == null) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationOf(leader.getUniqueId());
            if (n == null) {
                leader.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!leader.getUniqueId().equals(n.getLeaderUuid())) {
                leader.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uad6d\uac00\uc7a5\uc744 \uc704\uc784\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!n.isMember(newLeader)) {
                leader.sendMessage("\u00a7c\ub300\uc0c1\uc740 \uad6d\uac00\uc6d0\uc774\uc5b4\uc57c \ud569\ub2c8\ub2e4.");
                return false;
            }
            n.setLeaderUuid(newLeader);
            n.addOfficer(newLeader);
            this.store.saveAll();
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)newLeader);
        leader.sendMessage("\u00a7a\uad6d\uac00\uc7a5\uc744 " + (op.getName() != null ? op.getName() : newLeader.toString()) + " \uc5d0\uac8c \uc704\uc784\ud588\uc2b5\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean toggleRelocationMode(Player leader) {
        if (leader == null) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            boolean next;
            Nation n = this.store.getNationOf(leader.getUniqueId());
            if (n == null) {
                leader.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!leader.getUniqueId().equals(n.getLeaderUuid())) {
                leader.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            boolean bl = next = !n.isRelocationMode();
            if (next) {
                long cdMs = this.plugin.getConfig().getLong("nexus.relocation.cooldown-ms", 86400000L);
                long now = System.currentTimeMillis();
                long left = n.getLastRelocationAt() + cdMs - now;
                if (left > 0L) {
                    leader.sendMessage("\u00a7c\ub125\uc11c\uc2a4 \uc62e\uae30\uae30\ub294 \ucfe8\ud0c0\uc784\uc785\ub2c8\ub2e4. \ub0a8\uc740 \uc2dc\uac04: " + this.formatDuration(left));
                    return false;
                }
            }
            n.setRelocationMode(next);
            this.store.saveAll();
            leader.sendMessage(next ? "\u00a7a\ub125\uc11c\uc2a4 \uc62e\uae30\uae30 \ubaa8\ub4dc: ON" : "\u00a77\ub125\uc11c\uc2a4 \uc62e\uae30\uae30 \ubaa8\ub4dc: OFF");
            return true;
        }
    }

    private String formatDuration(long ms) {
        long sec = Math.max(0L, ms / 1000L);
        long h = sec / 3600L;
        long m = (sec %= 3600L) / 60L;
        sec %= 60L;
        if (h > 0L) {
            return h + "\uc2dc\uac04 " + m + "\ubd84";
        }
        if (m > 0L) {
            return m + "\ubd84 " + sec + "\ucd08";
        }
        return sec + "\ucd08";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean adminDisband(String nationName) {
        if (nationName == null || nationName.isBlank()) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationByName(nationName);
            if (n == null) {
                return false;
            }
            for (UUID u : new ArrayList<UUID>(n.getMembers())) {
                this.store.setNationOf(u, null);
            }
            this.store.removeNation(n.getName());
            this.store.saveAll();
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean upgrade(Player player) {
        Nation n = this.getNationOf(player.getUniqueId());
        if (n == null) {
            player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!n.isOfficer(player.getUniqueId())) {
            player.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uc5c5\uadf8\ub808\uc774\ub4dc\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        int max = this.plugin.getConfig().getInt("nation.max-level", 100);
        if (n.getLevel() >= max) {
            player.sendMessage("\u00a7c\uc774\ubbf8 \ucd5c\ub300 \ub808\ubca8\uc785\ub2c8\ub2e4.");
            return false;
        }
        long mult = this.plugin.getConfig().getLong("nation.upgrade-cost-multiplier", 5000L);
        long cost = (long)(n.getLevel() + 1) * mult;
        if (n.hasNexus() && this.territoryOverlapsOtherNations(n, n.getNexusWorld(), n.getNexusX(), n.getNexusZ(), n.getLevel() + 1)) {
            player.sendMessage("\u00a7c\uc5c5\uadf8\ub808\uc774\ub4dc \uc2dc \ubcf4\ud638\uad6c\uc5ed\uc774 \ub2e4\ub978 \uad6d\uac00\uc640 \uacb9\uce69\ub2c8\ub2e4. \uba3c\uc800 \ub125\uc11c\uc2a4 \uc704\uce58\ub97c \uc62e\uae30\uac70\ub098 \ub2e4\ub978 \uad6d\uac00\uc640 \uac70\ub9ac\ub97c \ub450\uc138\uc694.");
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            if (n.getTreasury() < cost) {
                player.sendMessage("\u00a7c\uad6d\uace0\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4. \ud544\uc694: " + cost);
                return false;
            }
            n.setTreasury(n.getTreasury() - cost);
            n.setLevel(n.getLevel() + 1);
            this.store.saveAll();
        }
        player.sendMessage("\u00a7a\uad6d\uac00 \ub808\ubca8 \uc5c5! \ud604\uc7ac \ub808\ubca8: " + n.getLevel());
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean depositToTreasury(Player player, long amount) {
        if (amount <= 0L) {
            return false;
        }
        Nation n = this.getNationOf(player.getUniqueId());
        if (n == null) {
            player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!n.isMember(player.getUniqueId())) {
            player.sendMessage("\u00a7c\uad6d\uac00\uc6d0\uc774 \uc544\ub2d9\ub2c8\ub2e4.");
            return false;
        }
        if (!this.moneyService.withdraw(player.getUniqueId(), amount)) {
            player.sendMessage("\u00a7c\ub3c8\uc774 \ubd80\uc871\ud569\ub2c8\ub2e4.");
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            n.setTreasury(n.getTreasury() + amount);
            String ts = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            n.addTreasuryLog("\u00a77[" + ts + "] \u00a7a\uc785\uae08 \u00a7f" + player.getName() + " \u00a77+" + amount);
            this.store.saveAll();
        }
        player.sendMessage("\u00a7a\uad6d\uace0\uc5d0 " + amount + " \uc785\uae08\ud588\uc2b5\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean adminWithdrawFromTreasury(String nationName, UUID target, long amount) {
        if (amount <= 0L) {
            return false;
        }
        if (nationName == null || nationName.isBlank()) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationByName(nationName);
            if (n == null) {
                return false;
            }
            if (n.getTreasury() < amount) {
                return false;
            }
            n.setTreasury(n.getTreasury() - amount);
            String ts = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            String who = target == null ? "?" : target.toString().substring(0, 8);
            n.addTreasuryLog("\u00a77[" + ts + "] \u00a7c\ucd9c\uae08(\uad00\ub9ac\uc790) \u00a7f" + who + " \u00a77-" + amount);
            this.moneyService.deposit(target, amount);
            this.store.saveAll();
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean toggleOfficer(Player leader, UUID member) {
        if (leader == null || member == null) {
            return false;
        }
        Nation n = this.getNationOf(leader.getUniqueId());
        if (n == null) {
            leader.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!leader.getUniqueId().equals(n.getLeaderUuid())) {
            leader.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uac04\ubd80\ub97c \uc9c0\uc815\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!n.isMember(member)) {
            leader.sendMessage("\u00a7c\ud574\ub2f9 \ud50c\ub808\uc774\uc5b4\ub294 \uad6d\uac00\uc6d0\uc774 \uc544\ub2d9\ub2c8\ub2e4.");
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            if (n.isOfficer(member) && !member.equals(n.getLeaderUuid())) {
                n.removeOfficer(member);
            } else {
                n.addOfficer(member);
            }
            this.store.saveAll();
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean upgradeTurret(Player player) {
        Nation n = this.getNationOf(player.getUniqueId());
        if (n == null) {
            player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!n.isOfficer(player.getUniqueId())) {
            player.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \ud3ec\ud0d1\uc744 \uac15\ud654\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        int max = this.plugin.getConfig().getInt("structures.turret.max-level", 10);
        if (n.getTurretLevel() >= max) {
            player.sendMessage("\u00a7c\ud3ec\ud0d1\uc774 \uc774\ubbf8 \ucd5c\ub300 \ub808\ubca8\uc785\ub2c8\ub2e4.");
            return false;
        }
        long mult = this.plugin.getConfig().getLong("structures.turret.upgrade-cost-multiplier", 5000L);
        long cost = (long)(n.getTurretLevel() + 1) * mult;
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            if (n.getTreasury() < cost) {
                player.sendMessage("\u00a7c\uad6d\uace0\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4. \ud544\uc694: " + cost);
                return false;
            }
            n.setTreasury(n.getTreasury() - cost);
            n.setTurretLevel(n.getTurretLevel() + 1);
            this.store.saveAll();
        }
        player.sendMessage("\u00a7a\ud3ec\ud0d1 \ub808\ubca8\uc774 " + n.getTurretLevel() + "\ub85c \uac15\ud654\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean upgradeInhibitor(Player player) {
        Nation n = this.getNationOf(player.getUniqueId());
        if (n == null) {
            player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        if (!n.isOfficer(player.getUniqueId())) {
            player.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uc5b5\uc81c\uae30\ub97c \uac15\ud654\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return false;
        }
        int max = this.plugin.getConfig().getInt("structures.inhibitor.max-level", 10);
        if (n.getInhibitorLevel() >= max) {
            player.sendMessage("\u00a7c\uc5b5\uc81c\uae30\uac00 \uc774\ubbf8 \ucd5c\ub300 \ub808\ubca8\uc785\ub2c8\ub2e4.");
            return false;
        }
        long mult = this.plugin.getConfig().getLong("structures.inhibitor.upgrade-cost-multiplier", 5000L);
        long cost = (long)(n.getInhibitorLevel() + 1) * mult;
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            if (n.getTreasury() < cost) {
                player.sendMessage("\u00a7c\uad6d\uace0\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4. \ud544\uc694: " + cost);
                return false;
            }
            n.setTreasury(n.getTreasury() - cost);
            n.setInhibitorLevel(n.getInhibitorLevel() + 1);
            this.store.saveAll();
        }
        player.sendMessage("\u00a7a\uc5b5\uc81c\uae30 \ub808\ubca8\uc774 " + n.getInhibitorLevel() + "\ub85c \uac15\ud654\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void runDailyUpkeep() {
        int arrearsThreshold = this.plugin.getConfig().getInt("nation.arrears-days-for-level-down", 5);
        long upkeepMult = this.plugin.getConfig().getLong("nation.daily-upkeep-multiplier", 1000L);
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            for (Nation n : this.store.getAllNations()) {
                long due = (long)n.getLevel() * upkeepMult;
                if (n.getTreasury() >= due) {
                    n.setTreasury(n.getTreasury() - due);
                    n.setArrearsDays(0);
                    continue;
                }
                n.setArrearsDays(n.getArrearsDays() + 1);
                while (n.getArrearsDays() >= arrearsThreshold && n.getLevel() > 1) {
                    n.setLevel(n.getLevel() - 1);
                    n.setArrearsDays(n.getArrearsDays() - arrearsThreshold);
                }
            }
            this.store.saveAll();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            Nation n;
            n = this.getNationOf(p.getUniqueId());
            if (n == null) continue;
            p.sendMessage("\u00a7e[\uad6d\uac00] \u00a7f\uc77c\uc77c \uc720\uc9c0\uae08 \uc815\uc0b0 \uc644\ub8cc. \uad6d\uace0: \u00a7a" + n.getTreasury() + " \u00a77(\ub808\ubca8 " + n.getLevel() + ")");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean adminResetUpkeep(String nationName) {
        if (nationName == null || nationName.isBlank()) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationByName(nationName);
            if (n == null) {
                return false;
            }
            n.setArrearsDays(0);
            this.store.saveNations();
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean dismantleNexus(Player player) {
        if (player == null) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            Nation n = this.store.getNationOf(player.getUniqueId());
            if (n == null) {
                player.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!player.getUniqueId().equals(n.getLeaderUuid())) {
                player.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \ub125\uc11c\uc2a4\ub97c \ucca0\uac70\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            if (!n.hasNexus()) {
                player.sendMessage("\u00a7c\ucca0\uac70\ud560 \ub125\uc11c\uc2a4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            World world = Bukkit.getWorld((String)n.getNexusWorld());
            if (world != null) {
                Location center = new Location(world, (double)n.getNexusX() + 0.5, (double)n.getNexusY() + 0.5, (double)n.getNexusZ() + 0.5);
                for (Entity ent : world.getNearbyEntities(center, 8.0, 8.0, 8.0)) {
                    try {
                        if (!ent.getScoreboardTags().contains("NW_STRUCTURE") || !ent.getScoreboardTags().contains("NW_TYPE_NEXUS") || !ent.getScoreboardTags().contains("NW_NATION_" + n.getName())) continue;
                        ent.remove();
                    }
                    catch (Exception exception) {}
                }
            }
            String nationName = n.getName();
            n.clearNexus();
            this.store.saveNations();
        }
        try {
            ItemStack nexusItem = Items.createNexusItem(this.plugin);
            HashMap leftover = player.getInventory().addItem(new ItemStack[]{nexusItem});
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), nexusItem);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        player.sendMessage("\u00a7e\ub125\uc11c\uc2a4\ub97c \ucca0\uac70\ud588\uc2b5\ub2c8\ub2e4. \ub2e4\uc2dc \uc124\uce58\ud558\uae30 \uc804\uae4c\uc9c0 \uad6d\uac00 \uae30\ub2a5\uc774 \uc7a0\uae08\ub429\ub2c8\ub2e4.");
        player.sendMessage("\u00a7a\ub125\uc11c\uc2a4 \uc7ac\uc124\uce58 \uc544\uc774\ud15c\uc774 \uc9c0\uae09\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
        return true;
    }
}

