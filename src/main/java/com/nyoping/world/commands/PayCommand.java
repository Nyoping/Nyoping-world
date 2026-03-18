package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public PayCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatUtils.error("사용법: /pay <플레이어> <금액>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[0]));
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(ChatUtils.error("자기 자신에게 송금할 수 없습니다."));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage(ChatUtils.error("0보다 큰 금액을 입력하세요."));
                return true;
            }

            if (plugin.getEconomyManager().transfer(player, target, amount)) {
                String formatted = plugin.getEconomyManager().formatMoney(amount);
                player.sendMessage(ChatUtils.success(target.getName() + "님에게 " + formatted + "을 송금했습니다."));
                target.sendMessage(ChatUtils.info(player.getName() + "님이 " + formatted + "을 송금했습니다."));
            } else {
                player.sendMessage(ChatUtils.error("잔액이 부족합니다."));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatUtils.error("올바른 금액을 입력하세요."));
        }
        return true;
    }
}
