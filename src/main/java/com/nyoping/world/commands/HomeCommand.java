package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class HomeCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public HomeCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        // /home - 홈 목록 또는 기본 홈으로 이동
        if (args.length == 0) {
            Set<String> homes = plugin.getTeleportManager().getHomeNames(player);
            if (homes.isEmpty()) {
                player.sendMessage(ChatUtils.error("설정된 홈이 없습니다. /sethome <이름>으로 설정하세요."));
                return true;
            }

            // 홈이 하나면 바로 이동
            if (homes.size() == 1) {
                String name = homes.iterator().next();
                Location loc = plugin.getTeleportManager().getHome(player, name);
                player.teleport(loc);
                player.sendMessage(ChatUtils.success("홈 '" + name + "'(으)로 이동했습니다."));
                return true;
            }

            // 여러 개면 목록 표시
            player.sendMessage(ChatUtils.info("=== 홈 목록 ==="));
            for (String name : homes) {
                player.sendMessage(ChatUtils.colorize("&7- &f" + name));
            }
            player.sendMessage(ChatUtils.colorize("&7이동: /home <이름>"));
            return true;
        }

        // /home <이름>
        String homeName = args[0].toLowerCase();
        Location loc = plugin.getTeleportManager().getHome(player, homeName);
        if (loc == null) {
            player.sendMessage(ChatUtils.error("홈 '" + homeName + "'을(를) 찾을 수 없습니다."));
            return true;
        }

        player.teleport(loc);
        player.sendMessage(ChatUtils.success("홈 '" + homeName + "'(으)로 이동했습니다."));
        return true;
    }
}
