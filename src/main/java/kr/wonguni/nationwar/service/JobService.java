/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.NationWarPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Bukkit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class JobService {
    public static final String JOB_GUI_TITLE = "\u00a70\uc9c1\uc5c5 \uc120\ud0dd";
    private final JavaPlugin plugin;
    private final DataStore store;

    public JobService(JavaPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Set<JobType> getJobs(UUID uuid) {
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            return new LinkedHashSet<JobType>(this.store.getOrCreatePlayer(uuid).getJobs());
        }
    }

    public void openJobGui(Player p) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)9, (String)JOB_GUI_TITLE);
        List<JobType> types = List.of(JobType.MINER, JobType.FARMER, JobType.COOK, JobType.FISHER, JobType.HUNTER, JobType.BREWER);
        int slot = 0;
        Set<JobType> current = this.getJobs(p.getUniqueId());
        for (JobType t : types) {
            ItemStack item = new ItemStack(this.iconOf(t));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((current.contains((Object)t) ? "\u00a7a" : "\u00a77") + JobService.koreanName(t));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("\u00a7f\ud074\ub9ad: " + (current.contains((Object)t) ? "\u00a7c\ud574\uc81c" : "\u00a7a\uc120\ud0dd"));
            lore.add("\u00a77\ucd5c\ub300 2\uac1c\uae4c\uc9c0 \uc120\ud0dd \uac00\ub2a5");
            PlayerProfile prof = this.store.getOrCreatePlayer(p.getUniqueId());
            int r = prof.getJobRank(t);
            lore.add("\u00a77\ub4f1\uae09: \u00a7f" + rankName(r));
            int profLevel = prof.getJobProficiencyLevel(t);
            int profXp = prof.getJobProficiency(t);
            int xpInLevel = profXp % PlayerProfile.XP_PER_LEVEL;
            lore.add("\u00a77\uc219\ub828\ub3c4: \u00a7fLv." + profLevel + " (" + xpInLevel + "/" + PlayerProfile.XP_PER_LEVEL + ")");
            long cdMs = this.plugin.getConfig().getLong("jobs.remove-cooldown-hours", 24L) * 3600L * 1000L;
            long remain = Math.max(0L, cdMs - (System.currentTimeMillis() - prof.getLastJobRemoveAt()));
            if (current.contains((Object)t)) {
                lore.add("\u00a77\uc81c\uac70 \ucfe8\ud0c0\uc784: \u00a7f" + (remain <= 0L ? "\uc81c\uac70 \uac00\ub2a5" : (this.formatRemaining(remain) + " \ub0a8\uc74c")));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta cm = clear.getItemMeta();
        cm.setDisplayName("\u00a7c\uc9c1\uc5c5 \ucd08\uae30\ud654(\ubc31\uc218)");
        clear.setItemMeta(cm);
        inv.setItem(8, clear);
        p.openInventory(inv);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void handleClick(Player p, int slot) {
        List<JobType> types = List.of(JobType.MINER, JobType.FARMER, JobType.COOK, JobType.FISHER, JobType.HUNTER, JobType.BREWER);
        if (slot == 8) {
            DataStore dataStore = this.store;
            synchronized (dataStore) {
                PlayerProfile prof = this.store.getOrCreatePlayer(p.getUniqueId());
                long cdMs = this.plugin.getConfig().getLong("jobs.remove-cooldown-hours", 24L) * 3600L * 1000L;
                long now = System.currentTimeMillis();
                long remain = Math.max(0L, cdMs - (now - prof.getLastJobRemoveAt()));
                if (remain > 0L) {
                    p.sendMessage("\u00a7c[\uc9c1\uc5c5] \u00a7f\uc81c\uac70 \ucfe8\ud0c0\uc784 \uc911\uc785\ub2c8\ub2e4. \ub0a8\uc740 \uc2dc\uac04: \u00a7e" + this.formatRemaining(remain));
                    return;
                }
                prof.setJobs(List.of(JobType.UNEMPLOYED));
                prof.setLastJobRemoveAt(now);
                prof.getJobRanks().clear();
                this.store.savePlayers();
                // reset advancements for all jobs
                JobQuestService q = ((NationWarPlugin)this.plugin).getJobQuestService();
                for (JobType jt : prof.getJobs()) {
                    if (jt == null || jt == JobType.UNEMPLOYED) continue;
                    q.resetJob(p, jt);
                }
                q.revokeRoot(p);
            }
            p.sendMessage("\u00a7e[\uc9c1\uc5c5] \u00a7f\uc9c1\uc5c5\uc774 \ucd08\uae30\ud654\ub418\uc5c8\uc2b5\ub2c8\ub2e4. (\ubc31\uc218)");
            this.openJobGui(p);
            return;
        }
        if (slot < 0 || slot >= types.size()) {
            return;
        }
        JobType clicked = types.get(slot);
        DataStore dataStore = this.store;
        synchronized (dataStore) {
            PlayerProfile prof = this.store.getOrCreatePlayer(p.getUniqueId());
            LinkedHashSet<JobType> jobs = new LinkedHashSet<JobType>(prof.getJobs());
            jobs.remove((Object)JobType.UNEMPLOYED);
            if (jobs.contains((Object)clicked)) {
                long cdMs = this.plugin.getConfig().getLong("jobs.remove-cooldown-hours", 24L) * 3600L * 1000L;
                long now = System.currentTimeMillis();
                long remain = Math.max(0L, cdMs - (now - prof.getLastJobRemoveAt()));
                if (remain > 0L) {
                    p.sendMessage("\u00a7c[\uc9c1\uc5c5] \u00a7f\uc81c\uac70 \ucfe8\ud0c0\uc784 \uc911\uc785\ub2c8\ub2e4. \ub0a8\uc740 \uc2dc\uac04: \u00a7e" + this.formatRemaining(remain));
                    return;
                }
                jobs.remove((Object)clicked);
                prof.setLastJobRemoveAt(now);
                prof.getJobRanks().remove(clicked);
                ((NationWarPlugin)this.plugin).getJobQuestService().resetJob(p, clicked);
            } else {
                int max = this.plugin.getConfig().getInt("jobs.max-per-player", 2);
                if (jobs.size() >= max) {
                    p.sendMessage("\u00a7c\ucd5c\ub300 " + max + "\uac1c\uae4c\uc9c0\ub9cc \uc120\ud0dd\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                jobs.add(clicked);
                prof.setJobRank(clicked, 0);
                ((NationWarPlugin)this.plugin).getJobQuestService().syncJobRank(p, clicked, 0);
            }
            if (jobs.isEmpty()) {
                jobs.add(JobType.UNEMPLOYED);
            }
            prof.setJobs(jobs);
            this.store.savePlayers();
        }
        this.openJobGui(p);
    }

    

private String formatRemaining(long ms) {
    if (ms <= 0L) return "0:00";
    long totalSec = ms / 1000L;
    long h = totalSec / 3600L;
    long m = (totalSec % 3600L) / 60L;
    return h + ":" + String.format("%02d", m);
}

private Material iconOf(JobType t) {
        return switch (t) {
            case JobType.MINER -> Material.IRON_PICKAXE;
            case JobType.FARMER -> Material.WHEAT;
            case JobType.COOK -> Material.COOKED_BEEF;
            case JobType.FISHER -> Material.FISHING_ROD;
            case JobType.HUNTER -> Material.BOW;
            case JobType.BREWER -> Material.BREWING_STAND;
            default -> Material.PAPER;
        };
    }

    private static final String[] RANK_NAMES = {
        "\uc2e0\uc785", "\ud3d0\uae09", "\ucd08\uae09", "\uc911\uae09",
        "\uc0c1\uae09", "\ucd5c\uc0c1\uae09", "\ud2b9\uae09", "\uc9ec\ud0b9"
    };

    public static String rankName(int rank) {
        if (rank < 0 || rank >= RANK_NAMES.length) return "???";
        return RANK_NAMES[rank];
    }

    public static String koreanName(JobType t) {
        return switch (t) {
            default -> throw new MatchException(null, null);
            case JobType.UNEMPLOYED -> "\ubc31\uc218";
            case JobType.MINER -> "\uad11\ubd80";
            case JobType.FARMER -> "\ub18d\ubd80";
            case JobType.COOK -> "\uc694\ub9ac\uc0ac";
            case JobType.FISHER -> "\uc5b4\ubd80";
            case JobType.HUNTER -> "\uc0ac\ub0e5\uafbc";
            case JobType.BREWER -> "\uc591\uc870\uc0ac";
        };
    }
}

