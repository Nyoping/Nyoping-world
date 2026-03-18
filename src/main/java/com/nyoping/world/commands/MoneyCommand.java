package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public MoneyCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        // /money - 자신의 잔액 확인
        if (args.length == 0) {
            double balance = plugin.getEconomyManager().getBalance(player);
            player.sendMessage(ChatUtils.info("잔액: " + plugin.getEconomyManager().formatMoney(balance)));
            return true;
        }

        // /money set <플레이어> <금액> - 관리자용
        if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
            if (!sender.hasPermission("nyoping.economy.admin")) {
                sender.sendMessage(ChatUtils.error("권한이 없습니다."));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[1]));
                return true;
            }

            try {
                double amount = Double.parseDouble(args[2]);
                plugin.getEconomyManager().setBalance(target, amount);
                sender.sendMessage(ChatUtils.success(target.getName() + "님의 잔액이 " +
                        plugin.getEconomyManager().formatMoney(amount) + "(으)로 설정되었습니다."));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtils.error("올바른 금액을 입력하세요."));
            }
            return true;
        }

        // /money give <플레이어> <금액> - 관리자용
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            if (!sender.hasPermission("nyoping.economy.admin")) {
                sender.sendMessage(ChatUtils.error("권한이 없습니다."));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[1]));
                return true;
            }

            try {
                double amount = Double.parseDouble(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(ChatUtils.error("0보다 큰 금액을 입력하세요."));
                    return true;
                }
                plugin.getEconomyManager().deposit(target, amount);
                sender.sendMessage(ChatUtils.success(target.getName() + "님에게 " +
                        plugin.getEconomyManager().formatMoney(amount) + "을 지급했습니다."));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtils.error("올바른 금액을 입력하세요."));
            }
            return true;
        }

        sender.sendMessage(ChatUtils.error("사용법: /money [set|give <플레이어> <금액>]"));
        return true;
    }
}
