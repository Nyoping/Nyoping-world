package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.listeners.ChatListener;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnmuteCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public UnmuteCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nyoping.mute")) {
            sender.sendMessage(ChatUtils.error("권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /unmute <플레이어>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[0]));
            return true;
        }

        ChatListener.mutedPlayers.remove(target.getUniqueId().toString());
        sender.sendMessage(ChatUtils.success(target.getName() + "님의 뮤트를 해제했습니다."));
        target.sendMessage(ChatUtils.success("채팅 금지가 해제되었습니다."));
        return true;
    }
}
