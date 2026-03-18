package kr.wonguni.nationwar.service;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WaterService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Entry> map = new HashMap<>();

    public WaterService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "water.yml");
        if (!this.file.exists()) {
            try { this.file.createNewFile(); } catch (IOException ignored) {}
        }
        load();
    }

    public static class Entry {
        public final String type; // "drink" | "salt"
        public final long time;
        public Entry(String type, long time) { this.type = type; this.time = time; }
    }

    private String key(World w, int x, int y, int z) {
        return w.getUID() + ":" + x + ":" + y + ":" + z;
    }

    public void record(Location loc, String type) {
        if (loc == null || loc.getWorld() == null || type == null) return;
        cleanup();
        int limit = plugin.getConfig().getInt("cook.water.record_limit", 10000);
        if (map.size() >= limit) {
            // drop oldest
            String oldestK = null;
            long oldestT = Long.MAX_VALUE;
            for (Map.Entry<String, Entry> e : map.entrySet()) {
                if (e.getValue().time < oldestT) { oldestT = e.getValue().time; oldestK = e.getKey(); }
            }
            if (oldestK != null) map.remove(oldestK);
        }
        map.put(key(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), new Entry(type, System.currentTimeMillis()));
    }

    public String get(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        cleanup();
        Entry e = map.get(key(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        return e == null ? null : e.type;
    }

    private void cleanup() {
        long ttlH = plugin.getConfig().getLong("cook.water.record_ttl_hours", 72L);
        long ttl = Math.max(1L, ttlH) * 3600_000L;
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(en -> now - en.getValue().time > ttl);
    }

    public void save() {
        cleanup();
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Entry> e : map.entrySet()) {
            y.set(e.getKey() + ".type", e.getValue().type);
            y.set(e.getKey() + ".time", e.getValue().time);
        }
        try { y.save(file); } catch (IOException ignored) {}
    }

    private void load() {
        map.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) {
            String type = y.getString(k + ".type", null);
            long time = y.getLong(k + ".time", 0L);
            if (type != null && time > 0) map.put(k, new Entry(type, time));
        }
        cleanup();
    }
}
