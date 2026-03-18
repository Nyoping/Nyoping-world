package com.nyoping.world.listeners;

import com.nyoping.world.NyopingWorld;
import com.nyoping.world.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class ChatListener implements Listener {

    private final NyopingWorld plugin;
    public static final Set<String> mutedPlayers = new HashSet<>();

    public ChatListener(NyopingWorld plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 뮤트 체크
        if (mutedPlayers.contains(player.getUniqueId().toString())) {
            event.setCancelled(true);
            player.sendMessage(ChatUtils.error("채팅이 금지된 상태입니다."));
            return;
        }

        // 채팅 포맷 설정
        String chatFormat = plugin.getConfig().getString("chat.format", "{prefix} {player}: {message}");
        String prefix = ChatUtils.colorize(plugin.getRankManager().getPlayerPrefix(player));

        String formatted = chatFormat
                .replace("{prefix}", prefix)
                .replace("{player}", "%1$s")
                .replace("{message}", "%2$s");

        event.setFormat(ChatUtils.colorize(formatted));
    }
}
