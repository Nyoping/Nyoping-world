package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public SetWarpCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (!player.hasPermission("nyoping.warp.set")) {
            player.sendMessage(ChatUtils.error("권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /setwarp <이름>"));
            return true;
        }

        String name = args[0].toLowerCase();
        plugin.getTeleportManager().setWarp(name, player.getLocation());
        player.sendMessage(ChatUtils.success("워프 '" + name + "'이(가) 설정되었습니다."));
        return true;
    }
}
