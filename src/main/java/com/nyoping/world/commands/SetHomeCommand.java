package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public SetHomeCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        String name = args.length > 0 ? args[0].toLowerCase() : "home";

        if (plugin.getTeleportManager().setHome(player, name)) {
            player.sendMessage(ChatUtils.success("홈 '" + name + "'이(가) 설정되었습니다."));
        } else {
            player.sendMessage(ChatUtils.error("홈 개수가 최대(" + plugin.getTeleportManager().getMaxHomes() + "개)입니다."));
        }
        return true;
    }
}
