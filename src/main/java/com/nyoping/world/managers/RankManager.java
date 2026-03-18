package com.nyoping.world.managers;

import com.nyoping.world.NyopingWorld;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RankManager {

    private final NyopingWorld plugin;
    private final File ranksFile;
    private FileConfiguration ranksConfig;
    private final File playerRanksFile;
    private FileConfiguration playerRanksConfig;

    // 랭크 이름 -> 권한 목록
    private final Map<String, List<String>> rankPermissions = new LinkedHashMap<>();
    // 랭크 이름 -> 접두사
    private final Map<String, String> rankPrefixes = new LinkedHashMap<>();
    // 플레이어 UUID -> 랭크 이름
    private final Map<String, String> playerRanks = new HashMap<>();

    public RankManager(NyopingWorld plugin) {
        this.plugin = plugin;
        this.ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        this.playerRanksFile = new File(plugin.getDataFolder(), "playerranks.yml");
        loadRanks();
        loadPlayerRanks();
    }

    private void loadRanks() {
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

        rankPermissions.clear();
        rankPrefixes.clear();

        if (ranksConfig.contains("ranks")) {
            for (String rankName : ranksConfig.getConfigurationSection("ranks").getKeys(false)) {
                String path = "ranks." + rankName;
                List<String> perms = ranksConfig.getStringList(path + ".permissions");
                String prefix = ranksConfig.getString(path + ".prefix", "&7[" + rankName + "]");
                rankPermissions.put(rankName, perms);
                rankPrefixes.put(rankName, prefix);
            }
        }
    }

    private void loadPlayerRanks() {
        if (!playerRanksFile.exists()) {
            try {
                playerRanksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("playerranks.yml 파일 생성 실패: " + e.getMessage());
            }
        }
        playerRanksConfig = YamlConfiguration.loadConfiguration(playerRanksFile);

        playerRanks.clear();
        if (playerRanksConfig.contains("players")) {
            for (String uuid : playerRanksConfig.getConfigurationSection("players").getKeys(false)) {
                playerRanks.put(uuid, playerRanksConfig.getString("players." + uuid));
            }
        }
    }

    public String getPlayerRank(Player player) {
        return playerRanks.getOrDefault(player.getUniqueId().toString(), getDefaultRank());
    }

    public String getPlayerRank(String uuid) {
        return playerRanks.getOrDefault(uuid, getDefaultRank());
    }

    public void setPlayerRank(Player player, String rank) {
        if (!rankPermissions.containsKey(rank)) return;

        String uuid = player.getUniqueId().toString();
        // 기존 권한 제거
        String oldRank = playerRanks.get(uuid);
        if (oldRank != null && rankPermissions.containsKey(oldRank)) {
            for (String perm : rankPermissions.get(oldRank)) {
                player.addAttachment(plugin, perm, false);
            }
        }

        playerRanks.put(uuid, rank);

        // 새 권한 적용
        for (String perm : rankPermissions.get(rank)) {
            player.addAttachment(plugin, perm, true);
        }
    }

    public String getPrefix(String rank) {
        return rankPrefixes.getOrDefault(rank, "&7[" + rank + "]");
    }

    public String getPlayerPrefix(Player player) {
        String rank = getPlayerRank(player);
        return getPrefix(rank);
    }

    public Set<String> getRankNames() {
        return rankPermissions.keySet();
    }

    public String getDefaultRank() {
        return plugin.getConfig().getString("ranks.default-rank", "member");
    }

    public void applyPermissions(Player player) {
        String rank = getPlayerRank(player);
        if (rankPermissions.containsKey(rank)) {
            for (String perm : rankPermissions.get(rank)) {
                player.addAttachment(plugin, perm, true);
            }
        }
    }

    public void saveAll() {
        playerRanksConfig = new YamlConfiguration();
        for (Map.Entry<String, String> entry : playerRanks.entrySet()) {
            playerRanksConfig.set("players." + entry.getKey(), entry.getValue());
        }
        try {
            playerRanksConfig.save(playerRanksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("playerranks.yml 저장 실패: " + e.getMessage());
        }
    }
}
