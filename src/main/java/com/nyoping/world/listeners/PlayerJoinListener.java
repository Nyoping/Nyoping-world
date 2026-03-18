package com.nyoping.world.listeners;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final NyopingWorld plugin;

    public PlayerJoinListener(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 경제 초기화
        plugin.getEconomyManager().initPlayer(player);

        // 권한 적용
        plugin.getRankManager().applyPermissions(player);

        // 입장 메시지
        String joinFormat = plugin.getConfig().getString("chat.join-format", "&e{player}님이 서버에 접속했습니다.");
        String prefix = ChatUtils.colorize(plugin.getRankManager().getPlayerPrefix(player));
        event.setJoinMessage(ChatUtils.colorize(joinFormat
                .replace("{player}", player.getName())
                .replace("{prefix}", prefix)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String quitFormat = plugin.getConfig().getString("chat.quit-format", "&7{player}님이 서버에서 나갔습니다.");
        event.setQuitMessage(ChatUtils.colorize(quitFormat.replace("{player}", player.getName())));
    }
}
