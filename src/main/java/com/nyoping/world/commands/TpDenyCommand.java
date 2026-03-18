package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpDenyCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public TpDenyCommand(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.error("플레이어만 사용할 수 있습니다."));
            return true;
        }

        String requesterUUID = plugin.getTeleportManager().getTpaRequester(player);
        if (requesterUUID == null) {
            player.sendMessage(ChatUtils.error("대기 중인 텔레포트 요청이 없습니다."));
            return true;
        }

        Player requester = Bukkit.getPlayer(UUID.fromString(requesterUUID));
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(ChatUtils.error(player.getName() + "님이 텔레포트 요청을 거절했습니다."));
        }

        player.sendMessage(ChatUtils.success("텔레포트 요청을 거절했습니다."));
        plugin.getTeleportManager().removeTpaRequest(player);
        return true;
    }
}
