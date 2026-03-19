/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.util.Set;
import java.util.stream.Collectors;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.service.JobService;
import kr.wonguni.nationwar.service.MoneyService;
import kr.wonguni.nationwar.service.NationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class HudService {
    private final JavaPlugin plugin;
    private final MoneyService moneyService;
    private final NationService nationService;
    private final JobService jobService;
    private final DataStore store;
    private int taskId = -1;

    public HudService(JavaPlugin plugin, DataStore store, MoneyService moneyService, NationService nationService, JobService jobService) {
        this.plugin = plugin;
        this.store = store;
        this.moneyService = moneyService;
        this.nationService = nationService;
        this.jobService = jobService;
    }

    public void start() {
        long period = this.plugin.getConfig().getLong("hud.actionbar-update-ticks", 20L);
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, this::tick, period, period);
    }

    public void stop() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerProfile prof;
            long money = this.moneyService.getBalance(p.getUniqueId());
            Nation n = this.nationService.getNationOf(p.getUniqueId());
            String nationName = n == null ? "\ubb34\uc18c\uc18d" : n.getName() + "(Lv." + n.getLevel() + ")";
            Set<JobType> jobs = this.jobService.getJobs(p.getUniqueId());
            String jobText = jobs.stream().filter(j -> j != JobType.UNEMPLOYED).map(JobService::koreanName).collect(Collectors.joining(","));
            if (jobText.isBlank()) {
                jobText = "\ubc31\uc218";
            }
            DataStore dataStore = this.store;
            synchronized (dataStore) {
                prof = this.store.getOrCreatePlayer(p.getUniqueId());
            }
            int cl = prof != null ? prof.getCombatLevel() : 0;
            int k = prof != null ? prof.getKills() : 0;
            int d = prof != null ? prof.getDeaths() : 0;
            int a = prof != null ? prof.getAssists() : 0;
            String msg = "\u00a7e\ub3c8\u00a7f:" + money + "  \u00a7b\uad6d\uac00\u00a7f:" + nationName + "  \u00a7d\uc9c1\uc5c5\u00a7f:" + jobText + "  \u00a7c\uc804\ud22cLv\u00a7f:" + cl + "  \u00a7fK/D/A:" + k + "/" + d + "/" + a;
            p.sendActionBar((Component)Component.text((String)msg));
        }
    }
}

