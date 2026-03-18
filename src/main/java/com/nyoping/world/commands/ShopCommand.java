package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ShopCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public ShopCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("buy")) {
            return handleBuy(player, args);
        }

        // 상점 목록 표시
        player.sendMessage(ChatUtils.info("=== 상점 ==="));
        List<String> items = plugin.getConfig().getStringList("economy.shop-items");
        for (String item : items) {
            String[] parts = item.split(":");
            if (parts.length >= 2) {
                String itemName = parts[0];
                String price = parts[1];
                player.sendMessage(ChatUtils.colorize("&7- &f" + itemName + " &7| &e" +
                        plugin.getEconomyManager().formatMoney(Double.parseDouble(price))));
            }
        }
        player.sendMessage(ChatUtils.colorize("&7구매: /shop buy <아이템이름> [수량]"));
        return true;
    }

    private boolean handleBuy(Player player, String[] args) {
        String itemName = args[1].toUpperCase();
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    player.sendMessage(ChatUtils.error("수량은 1~64 사이여야 합니다."));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatUtils.error("올바른 수량을 입력하세요."));
                return true;
            }
        }

        List<String> items = plugin.getConfig().getStringList("economy.shop-items");
        for (String item : items) {
            String[] parts = item.split(":");
            if (parts.length >= 2 && parts[0].equalsIgnoreCase(itemName)) {
                double price = Double.parseDouble(parts[1]) * amount;
                Material material = Material.matchMaterial(itemName);
                if (material == null) {
                    player.sendMessage(ChatUtils.error("존재하지 않는 아이템입니다."));
                    return true;
                }

                if (plugin.getEconomyManager().withdraw(player, price)) {
                    player.getInventory().addItem(new ItemStack(material, amount));
                    player.sendMessage(ChatUtils.success(itemName + " x" + amount + "을(를) " +
                            plugin.getEconomyManager().formatMoney(price) + "에 구매했습니다."));
                } else {
                    player.sendMessage(ChatUtils.error("잔액이 부족합니다. (필요: " +
                            plugin.getEconomyManager().formatMoney(price) + ")"));
                }
                return true;
            }
        }

        player.sendMessage(ChatUtils.error("상점에 없는 아이템입니다: " + itemName));
        return true;
    }
}
