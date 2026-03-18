package com.nyoping.world.commands;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpAcceptCommand implements CommandExecutor {

    private final NyopingWorld plugin;

    public TpAcceptCommand(NyopingWorld plugin) {
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
        if (requester == null || !requester.isOnline()) {
            player.sendMessage(ChatUtils.error("요청한 플레이어가 오프라인입니다."));
            plugin.getTeleportManager().removeTpaRequest(player);
            return true;
        }

        requester.teleport(player.getLocation());
        requester.sendMessage(ChatUtils.success(player.getName() + "님에게 텔레포트했습니다."));
        player.sendMessage(ChatUtils.success("텔레포트 요청을 수락했습니다."));
        plugin.getTeleportManager().removeTpaRequest(player);
        return true;
    }
}
