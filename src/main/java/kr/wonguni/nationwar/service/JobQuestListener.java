package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JobQuestListener implements Listener {
    private final DataStore store;
    private final JobQuestService quests;

    public JobQuestListener(DataStore store, JobQuestService quests) {
        this.store = store;
        this.quests = quests;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerProfile prof = this.store.getOrCreatePlayer(p.getUniqueId());
        this.quests.getPlugin().getServer().getScheduler().runTaskLater(this.quests.getPlugin(), () -> {
            this.quests.syncPlayer(p, prof);
        }, 20L);
    }
}
