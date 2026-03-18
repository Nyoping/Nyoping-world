/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.model.StatType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class StatService {
    private final JavaPlugin plugin;
    private final DataStore store;

    public StatService(JavaPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                try {
                    this.applyHungerTick(p);
                }
                catch (Throwable throwable) {}
            }
        }, 40L, 40L);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PlayerProfile getProfile(UUID uuid) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            return this.store.getOrCreatePlayer(uuid);
        }
    }

    public int getStatPoints(UUID uuid) {
        return this.getProfile(uuid).getStatPoints();
    }

    public Map<StatType, Integer> getStats(UUID uuid) {
        PlayerProfile p = this.getProfile(uuid);
        EnumMap<StatType, Integer> m = new EnumMap<StatType, Integer>(StatType.class);
        for (StatType t : StatType.values()) {
            m.put(t, p.getStat(t));
        }
        return m;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean upgrade(Player player, StatType type) {
        if (player == null || type == null) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile prof = this.store.getOrCreatePlayer(player.getUniqueId());
            if (prof.getStatPoints() <= 0) {
                return false;
            }
            prof.setStatPoints(prof.getStatPoints() - 1);
            prof.setStat(type, prof.getStat(type) + 1);
            prof.addCombatLevel(1);
            this.store.savePlayers();
        }
        this.syncStatDisplay(player);
        this.apply(player);
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void apply(Player player) {
        if (player == null) {
            return;
        }
        PlayerProfile prof = this.getProfile(player.getUniqueId());
        int health = prof.getStat(StatType.HEALTH);
        int hunger = prof.getStat(StatType.HUNGER);
        double base = 20.0;
        double add = Math.min((double)health * 2.0, 80.0);
        double max = base + add;
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);
            if (player.getHealth() > max) {
                player.setHealth(max);
            }
        }
        int maxReserve = Math.max(0, prof.getStat(StatType.HUNGER));
        if (prof.getHungerReserve() > maxReserve) {
            DataStore dataStore = this.store;
            synchronized (dataStore) {
                PlayerProfile p2 = this.store.getOrCreatePlayer(player.getUniqueId());
                p2.setHungerReserve(Math.min(p2.getHungerReserve(), maxReserve));
                this.store.savePlayers();
            }
        }
    }

    public void syncStatDisplay(Player p) {
        int per = (int)this.plugin.getConfig().getLong("stat.xp-per-point", 100L);
        if (per <= 0) {
            per = 100;
        }
        PlayerProfile prof = this.getProfile(p.getUniqueId());
        int points = prof.getStatPoints();
        float progress = (float)prof.getStatXpRemainder() / (float)per;
        p.setTotalExperience(0);
        p.setLevel(points);
        p.setExp(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void adminAddPoints(Player target, int delta) {
        if (target == null) {
            return;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile prof = this.store.getOrCreatePlayer(target.getUniqueId());
            prof.setStatPoints(Math.max(0, prof.getStatPoints() + delta));
            this.store.savePlayers();
        }
        this.syncStatDisplay(target);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void adminSetPoints(Player target, int points) {
        if (target == null) {
            return;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile prof = this.store.getOrCreatePlayer(target.getUniqueId());
            prof.setStatPoints(Math.max(0, points));
            prof.setStatXpRemainder(0);
            this.store.savePlayers();
        }
        this.syncStatDisplay(target);
    }

    private void applyHungerTick(Player player) {
        int maxFood;
        if (player == null) {
            return;
        }
        PlayerProfile prof = this.getProfile(player.getUniqueId());
        int hungerStat = prof.getStat(StatType.HUNGER);
        if (hungerStat <= 0) {
            return;
        }
        int maxReserve = Math.max(0, hungerStat);
        int reserve = Math.min(Math.max(0, prof.getHungerReserve()), maxReserve);
        int food = player.getFoodLevel();
        int totalFood = food + reserve;
        if (totalFood < (maxFood = 20 + hungerStat) && food >= 20) {
            // player.setFoodLevel(19); // 19로 강제 설정하는 로직 제거
        }
    }

    public int getTotalFood(Player player) {
        if (player == null) {
            return 0;
        }
        PlayerProfile prof = this.getProfile(player.getUniqueId());
        int reserve = Math.max(0, prof.getHungerReserve());
        int food = player.getFoodLevel();
        return food + reserve;
    }

    public int getMaxFood(Player player) {
        if (player == null) {
            return 20;
        }
        PlayerProfile prof = this.getProfile(player.getUniqueId());
        return 20 + prof.getStat(StatType.HUNGER);
    }
}

