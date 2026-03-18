package com.nyoping.world.managers;

import com.nyoping.world.NyopingWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeleportManager {

    private final NyopingWorld plugin;
    private final File homesFile;
    private final File warpsFile;
    private FileConfiguration homesConfig;
    private FileConfiguration warpsConfig;

    // 플레이어 UUID -> (홈이름 -> 위치)
    private final Map<String, Map<String, Location>> playerHomes = new HashMap<>();
    // 워프 이름 -> 위치
    private final Map<String, Location> warps = new LinkedHashMap<>();
    // TPA 요청: 요청자 UUID -> 대상 UUID
    private final Map<String, String> tpaRequests = new HashMap<>();

    private final int maxHomes;

    public TeleportManager(NyopingWorld plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        this.maxHomes = plugin.getConfig().getInt("teleport.max-homes", 3);
        loadHomes();
        loadWarps();
    }

    private void loadHomes() {
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("homes.yml 파일 생성 실패: " + e.getMessage());
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);

        playerHomes.clear();
        if (homesConfig.contains("homes")) {
            for (String uuid : homesConfig.getConfigurationSection("homes").getKeys(false)) {
                Map<String, Location> homes = new HashMap<>();
                for (String homeName : homesConfig.getConfigurationSection("homes." + uuid).getKeys(false)) {
                    Location loc = deserializeLocation("homes." + uuid + "." + homeName, homesConfig);
                    if (loc != null) homes.put(homeName, loc);
                }
                playerHomes.put(uuid, homes);
            }
        }
    }

    private void loadWarps() {
        if (!warpsFile.exists()) {
            try {
                warpsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("warps.yml 파일 생성 실패: " + e.getMessage());
            }
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);

        warps.clear();
        if (warpsConfig.contains("warps")) {
            for (String warpName : warpsConfig.getConfigurationSection("warps").getKeys(false)) {
                Location loc = deserializeLocation("warps." + warpName, warpsConfig);
                if (loc != null) warps.put(warpName, loc);
            }
        }
    }

    // --- 홈 관련 ---

    public boolean setHome(Player player, String name) {
        String uuid = player.getUniqueId().toString();
        Map<String, Location> homes = playerHomes.computeIfAbsent(uuid, k -> new HashMap<>());

        if (!homes.containsKey(name) && homes.size() >= maxHomes) {
            return false;
        }

        homes.put(name, player.getLocation());
        return true;
    }

    public Location getHome(Player player, String name) {
        String uuid = player.getUniqueId().toString();
        Map<String, Location> homes = playerHomes.get(uuid);
        return homes != null ? homes.get(name) : null;
    }

    public boolean deleteHome(Player player, String name) {
        String uuid = player.getUniqueId().toString();
        Map<String, Location> homes = playerHomes.get(uuid);
        if (homes == null) return false;
        return homes.remove(name) != null;
    }

    public Set<String> getHomeNames(Player player) {
        String uuid = player.getUniqueId().toString();
        Map<String, Location> homes = playerHomes.get(uuid);
        return homes != null ? homes.keySet() : Collections.emptySet();
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    // --- 워프 관련 ---

    public void setWarp(String name, Location location) {
        warps.put(name, location);
    }

    public Location getWarp(String name) {
        return warps.get(name);
    }

    public boolean deleteWarp(String name) {
        return warps.remove(name) != null;
    }

    public Set<String> getWarpNames() {
        return warps.keySet();
    }

    // --- TPA 관련 ---

    public void sendTpaRequest(Player requester, Player target) {
        tpaRequests.put(target.getUniqueId().toString(), requester.getUniqueId().toString());

        // 60초 후 자동 만료
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tpaRequests.remove(target.getUniqueId().toString());
        }, 20 * 60L);
    }

    public String getTpaRequester(Player target) {
        return tpaRequests.get(target.getUniqueId().toString());
    }

    public void removeTpaRequest(Player target) {
        tpaRequests.remove(target.getUniqueId().toString());
    }

    // --- 직렬화 유틸 ---

    private void serializeLocation(String path, Location loc, FileConfiguration config) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", (double) loc.getYaw());
        config.set(path + ".pitch", (double) loc.getPitch());
    }

    private Location deserializeLocation(String path, FileConfiguration config) {
        String worldName = config.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }

    public void saveAll() {
        // 홈 저장
        homesConfig = new YamlConfiguration();
        for (Map.Entry<String, Map<String, Location>> entry : playerHomes.entrySet()) {
            for (Map.Entry<String, Location> homeEntry : entry.getValue().entrySet()) {
                serializeLocation("homes." + entry.getKey() + "." + homeEntry.getKey(),
                        homeEntry.getValue(), homesConfig);
            }
        }
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("homes.yml 저장 실패: " + e.getMessage());
        }

        // 워프 저장
        warpsConfig = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            serializeLocation("warps." + entry.getKey(), entry.getValue(), warpsConfig);
        }
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("warps.yml 저장 실패: " + e.getMessage());
        }
    }
}
