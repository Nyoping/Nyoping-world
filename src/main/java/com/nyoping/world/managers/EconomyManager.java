package com.nyoping.world.managers;

import com.nyoping.world.NyopingWorld;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final NyopingWorld plugin;
    private final File economyFile;
    private FileConfiguration economyConfig;
    private final Map<String, Double> balances = new HashMap<>();
    private final String currencyName;
    private final double startingBalance;

    public EconomyManager(NyopingWorld plugin) {
        this.plugin = plugin;
        this.economyFile = new File(plugin.getDataFolder(), "economy.yml");
        this.currencyName = plugin.getConfig().getString("economy.currency-name", "원");
        this.startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 1000.0);
        loadEconomy();
    }

    private void loadEconomy() {
        if (!economyFile.exists()) {
            try {
                economyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("economy.yml 파일 생성 실패: " + e.getMessage());
            }
        }
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);

        balances.clear();
        if (economyConfig.contains("balances")) {
            for (String uuid : economyConfig.getConfigurationSection("balances").getKeys(false)) {
                balances.put(uuid, economyConfig.getDouble("balances." + uuid));
            }
        }
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId().toString());
    }

    public double getBalance(String uuid) {
        return balances.getOrDefault(uuid, startingBalance);
    }

    public void setBalance(Player player, double amount) {
        balances.put(player.getUniqueId().toString(), amount);
    }

    public void setBalance(String uuid, double amount) {
        balances.put(uuid, amount);
    }

    public boolean withdraw(Player player, double amount) {
        double balance = getBalance(player);
        if (balance < amount) return false;
        setBalance(player, balance - amount);
        return true;
    }

    public void deposit(Player player, double amount) {
        setBalance(player, getBalance(player) + amount);
    }

    public boolean transfer(Player from, Player to, double amount) {
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    public void initPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, startingBalance);
        }
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String formatMoney(double amount) {
        return String.format("%,.0f%s", amount, currencyName);
    }

    public void saveAll() {
        economyConfig = new YamlConfiguration();
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            economyConfig.set("balances." + entry.getKey(), entry.getValue());
        }
        try {
            economyConfig.save(economyFile);
        } catch (IOException e) {
            plugin.getLogger().warning("economy.yml 저장 실패: " + e.getMessage());
        }
    }
}
