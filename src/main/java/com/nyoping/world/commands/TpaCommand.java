package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public TpaCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.error("사용법: /tpa <플레이어>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtils.error("플레이어를 찾을 수 없습니다: " + args[0]));
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(ChatUtils.error("자기 자신에게 TPA를 보낼 수 없습니다."));
            return true;
        }

        plugin.getTeleportManager().sendTpaRequest(player, target);
        player.sendMessage(ChatUtils.success(target.getName() + "님에게 텔레포트 요청을 보냈습니다."));
        target.sendMessage(ChatUtils.info(player.getName() + "님이 텔레포트 요청을 보냈습니다."));
        target.sendMessage(ChatUtils.colorize("&a/tpaccept &7- 수락 | &c/tpdeny &7- 거절"));
        return true;
    }
}
