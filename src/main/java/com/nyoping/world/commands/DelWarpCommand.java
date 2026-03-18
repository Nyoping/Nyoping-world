package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelWarpCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public DelWarpCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nyoping.warp.delete")) {
            sender.sendMessage(ChatUtils.error("권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /delwarp <이름>"));
            return true;
        }

        String name = args[0].toLowerCase();
        if (plugin.getTeleportManager().deleteWarp(name)) {
            sender.sendMessage(ChatUtils.success("워프 '" + name + "'이(가) 삭제되었습니다."));
        } else {
            sender.sendMessage(ChatUtils.error("워프 '" + name + "'을(를) 찾을 수 없습니다."));
        }
        return true;
    }
}
