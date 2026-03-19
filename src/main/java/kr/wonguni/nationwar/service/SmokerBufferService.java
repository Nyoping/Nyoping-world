package kr.wonguni.nationwar.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Smoker;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SmokerBufferService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Deque<ItemStack>> buffers = new HashMap<>();

    public SmokerBufferService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "smoker_buffer.yml");
        if (!this.file.exists()) {
            try { this.file.createNewFile(); } catch (IOException ignored) {}
        }
        load();
    }

    private String key(Block b) {
        return b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    public void push(Block smokerBlock, ItemStack it) {
        if (smokerBlock == null || it == null || it.getType() == Material.AIR) return;
        String k = key(smokerBlock);
        buffers.computeIfAbsent(k, kk -> new ArrayDeque<>()).addLast(it.clone());
    }

    public void tick() {
        // iterate known smoker buffers, feed if possible
        Iterator<Map.Entry<String, Deque<ItemStack>>> it = buffers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<ItemStack>> e = it.next();
            Deque<ItemStack> q = e.getValue();
            if (q == null || q.isEmpty()) {
                it.remove();
                continue;
            }
            Block b = blockFromKey(e.getKey());
            if (b == null || b.getType() != Material.SMOKER) continue;
            if (!(b.getState() instanceof Smoker sm)) continue;

            Inventory inv = sm.getInventory();
            ItemStack input = inv.getItem(0);
            if (input != null && input.getType() != Material.AIR) continue;

            ItemStack next = q.peekFirst();
            if (next == null || next.getType() == Material.AIR) {
                q.pollFirst();
                continue;
            }
            inv.setItem(0, next);
            q.pollFirst();
            sm.update(true, false);
        }
    }

    private Block blockFromKey(String k) {
        try {
            String[] s = k.split(":");
            if (s.length != 4) return null;
            UUID worldId = UUID.fromString(s[0]);
            int x = Integer.parseInt(s[1]);
            int y = Integer.parseInt(s[2]);
            int z = Integer.parseInt(s[3]);
            org.bukkit.World w = null;
            for (org.bukkit.World ww : Bukkit.getWorlds()) {
                if (ww.getUID().equals(worldId)) { w = ww; break; }
            }
            if (w == null) return null;
            return w.getBlockAt(x, y, z);
        } catch (Exception ex) {
            return null;
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Deque<ItemStack>> e : buffers.entrySet()) {
            List<ItemStack> list = new ArrayList<>(e.getValue());
            y.set(e.getKey(), list);
        }
        try { y.save(file); } catch (IOException ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void load() {
        buffers.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) {
            List<?> raw = y.getList(k);
            if (raw == null) continue;
            Deque<ItemStack> q = new ArrayDeque<>();
            for (Object o : raw) {
                if (o instanceof ItemStack is) q.addLast(is);
            }
            if (!q.isEmpty()) buffers.put(k, q);
        }
    }
}
