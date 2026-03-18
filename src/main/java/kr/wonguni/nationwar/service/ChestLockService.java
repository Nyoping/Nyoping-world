/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.service.NationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestLockService {
    private final JavaPlugin plugin;
    private final NationService nationService;
    private final File file;
    private FileConfiguration cfg;
    private final Map<String, LockData> locks = new HashMap<String, LockData>();

    public ChestLockService(JavaPlugin plugin, NationService nationService) {
        this.plugin = plugin;
        this.nationService = nationService;
        this.file = new File(plugin.getDataFolder(), "locks.yml");
        this.load();
    }

    public void load() {
        if (!this.file.exists()) {
            try {
                this.file.getParentFile().mkdirs();
                this.file.createNewFile();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.cfg = YamlConfiguration.loadConfiguration((File)this.file);
        this.locks.clear();
        ConfigurationSection root = this.cfg.getConfigurationSection("locks");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            String nation;
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null || (nation = s.getString("nation", "")).isEmpty()) continue;
            String ownerStr = s.getString("owner", null);
            UUID owner = null;
            if (ownerStr != null && !ownerStr.isEmpty()) {
                try {
                    owner = UUID.fromString(ownerStr);
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
            LockData d = new LockData(nation, owner);
            d.locked = s.getBoolean("locked", true);
            List<String> list = s.getStringList("allowed");
            for (String uu : list) {
                try {
                    d.allowed.add(UUID.fromString(uu));
                }
                catch (Exception exception) {}
            }
            this.locks.put(key, d);
        }
    }

    public void save() {
        this.cfg.set("locks", null);
        for (Map.Entry<String, LockData> en : this.locks.entrySet()) {
            String base = "locks." + en.getKey();
            LockData d = en.getValue();
            this.cfg.set(base + ".nation", (Object)d.nationName);
            this.cfg.set(base + ".owner", (Object)(d.owner == null ? "" : d.owner.toString()));
            this.cfg.set(base + ".locked", (Object)d.locked);
            ArrayList<String> al = new ArrayList<String>();
            for (UUID u : d.allowed) {
                al.add(u.toString());
            }
            this.cfg.set(base + ".allowed", al);
        }
        try {
            this.cfg.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save locks.yml: " + e.getMessage());
        }
    }

    public String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public LockData get(Location loc) {
        return this.locks.get(this.key(loc));
    }

    public LockData getOrCreate(Location loc, String nationName, UUID creator) {
        String k = this.key(loc);
        LockData d = this.locks.get(k);
        if (d == null) {
            d = new LockData(nationName, creator);
            d.locked = true;
            this.locks.put(k, d);
            this.save();
        }
        if (d.owner == null && creator != null) {
            d.owner = creator;
            this.save();
        }
        return d;
    }

    public void remove(Location loc) {
        this.locks.remove(this.key(loc));
        this.save();
    }

    public boolean canOpen(UUID player, Location loc) {
        LockData d = this.get(loc);
        if (d == null) {
            return true;
        }
        if (!d.locked) {
            return true;
        }
        if (d.owner != null && player.equals(d.owner)) {
            return true;
        }
        return d.allowed.contains(player);
    }

    public List<OfflinePlayer> listNationMembers(String nationName) {
        Nation n = this.nationService.getNationByName(nationName);
        if (n == null) {
            return List.of();
        }
        ArrayList<OfflinePlayer> out = new ArrayList<OfflinePlayer>();
        for (UUID u : n.getMembers()) {
            out.add(Bukkit.getOfflinePlayer((UUID)u));
        }
        out.sort(Comparator.comparing(op -> op.getName() == null ? "" : op.getName(), String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static final class LockData {
        public final String nationName;
        public UUID owner;
        public boolean locked;
        public final Set<UUID> allowed = new HashSet<UUID>();

        public LockData(String nationName, UUID owner) {
            this.nationName = nationName;
            this.owner = owner;
        }
    }
}

