package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public RankCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nyoping.rank")) {
            sender.sendMessage(ChatUtils.error("권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.info("=== 랭크 목록 ==="));
            for (String rank : plugin.getRankManager().getRankNames()) {
                String prefix = ChatUtils.colorize(plugin.getRankManager().getPrefix(rank));
                sender.sendMessage(ChatUtils.colorize("&7- &f" + rank + " " + prefix));
            }
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatUtils.error("사용법: /rank set <플레이어> <랭크>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[1]));
            return true;
        }

        String rankName = args[2].toLowerCase();
        if (!plugin.getRankManager().getRankNames().contains(rankName)) {
            sender.sendMessage(ChatUtils.error("존재하지 않는 랭크입니다: " + rankName));
            return true;
        }

        plugin.getRankManager().setPlayerRank(target, rankName);
        sender.sendMessage(ChatUtils.success(target.getName() + "님의 랭크가 " + rankName + "(으)로 설정되었습니다."));
        target.sendMessage(ChatUtils.info("당신의 랭크가 " + rankName + "(으)로 변경되었습니다."));
        return true;
    }
}
