/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MoneyService {
    private final JavaPlugin plugin;
    private final DataStore store;

    public MoneyService(JavaPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, this.store::saveAll, 2400L, 2400L);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long getBalance(UUID uuid) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            return this.store.getOrCreatePlayer(uuid).getBalance();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setBalance(UUID uuid, long amount) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile p = this.store.getOrCreatePlayer(uuid);
            p.setBalance(amount);
            this.store.saveAll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean deposit(UUID uuid, long amount) {
        if (amount <= 0L) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile p = this.store.getOrCreatePlayer(uuid);
            p.setBalance(p.getBalance() + amount);
            this.store.saveAll();
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean withdraw(UUID uuid, long amount) {
        if (amount <= 0L) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile p = this.store.getOrCreatePlayer(uuid);
            if (p.getBalance() < amount) {
                return false;
            }
            p.setBalance(p.getBalance() - amount);
            this.store.saveAll();
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean transfer(UUID from, UUID to, long amount) {
        if (amount <= 0L) {
            return false;
        }
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile pf = this.store.getOrCreatePlayer(from);
            if (pf.getBalance() < amount) {
                return false;
            }
            pf.setBalance(pf.getBalance() - amount);
            PlayerProfile pt = this.store.getOrCreatePlayer(to);
            pt.setBalance(pt.getBalance() + amount);
            this.store.saveAll();
            return true;
        }
    }

    public void notifyBalance(Player player) {
        long bal = this.getBalance(player.getUniqueId());
        player.sendMessage("\u00a7e[\ub3c8] \u00a7f\ubcf4\uc720\uae08: \u00a7a" + bal);
    }
}

