package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.listeners.ChatListener;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public MuteCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nyoping.mute")) {
            sender.sendMessage(ChatUtils.error("권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /mute <플레이어>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[0]));
            return true;
        }

        ChatListener.mutedPlayers.add(target.getUniqueId().toString());
        sender.sendMessage(ChatUtils.success(target.getName() + "님을 뮤트했습니다."));
        target.sendMessage(ChatUtils.error("관리자에 의해 채팅이 금지되었습니다."));
        return true;
    }
}
