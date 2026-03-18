package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BroadcastCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public BroadcastCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nyoping.broadcast")) {
            sender.sendMessage(ChatUtils.error("권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /broadcast <메시지>"));
            return true;
        }

        String message = String.join(" ", args);
        String broadcastPrefix = plugin.getConfig().getString("chat.broadcast-prefix", "&c[공지]");
        Bukkit.broadcastMessage(ChatUtils.colorize(broadcastPrefix + " &f" + message));
        return true;
    }
}
