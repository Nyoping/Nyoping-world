package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public DelHomeCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /delhome <이름>"));
            return true;
        }

        String name = args[0].toLowerCase();
        if (plugin.getTeleportManager().deleteHome(player, name)) {
            player.sendMessage(ChatUtils.success("홈 '" + name + "'이(가) 삭제되었습니다."));
        } else {
            player.sendMessage(ChatUtils.error("홈 '" + name + "'을(를) 찾을 수 없습니다."));
        }
        return true;
    }
}
