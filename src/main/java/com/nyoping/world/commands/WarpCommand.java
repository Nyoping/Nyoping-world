package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class WarpCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public WarpCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (args.length == 0) {
            Set<String> warps = plugin.getTeleportManager().getWarpNames();
            if (warps.isEmpty()) {
                player.sendMessage(ChatUtils.info("설정된 워프가 없습니다."));
                return true;
            }
            player.sendMessage(ChatUtils.info("=== 워프 목록 ==="));
            for (String name : warps) {
                player.sendMessage(ChatUtils.colorize("&7- &f" + name));
            }
            player.sendMessage(ChatUtils.colorize("&7이동: /warp <이름>"));
            return true;
        }

        String warpName = args[0].toLowerCase();
        Location loc = plugin.getTeleportManager().getWarp(warpName);
        if (loc == null) {
            player.sendMessage(ChatUtils.error("워프 '" + warpName + "'을(를) 찾을 수 없습니다."));
            return true;
        }

        player.teleport(loc);
        player.sendMessage(ChatUtils.success("워프 '" + warpName + "'(으)로 이동했습니다."));
        return true;
    }
}
