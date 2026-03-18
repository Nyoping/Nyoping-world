package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

public class MinerHasteService implements Runnable {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final JobService jobs;

    public MinerHasteService(JavaPlugin plugin, DataStore store, JobService jobs) {
        this.plugin = plugin;
        this.store = store;
        this.jobs = jobs;
    }

    private boolean isPickaxe(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_PICKAXE");
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!jobs.getJobs(p.getUniqueId()).contains(JobType.MINER)) {
                p.removePotionEffect(PotionEffectType.HASTE);
                continue;
            }
            Material hand = p.getInventory().getItemInMainHand().getType();
            if (!isPickaxe(hand)) {
                p.removePotionEffect(PotionEffectType.HASTE);
                continue;
            }
            PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
            int rank = Math.max(0, Math.min(7, prof.getJobRank(JobType.MINER)));
            int amp = plugin.getConfig().getInt("miner.haste_amplifier_by_rank." + rank, rank);
            if (amp < 0) amp = 0;
            if (amp > 7) amp = 7;
            p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, amp, true, false, false));
        }
    }
}
