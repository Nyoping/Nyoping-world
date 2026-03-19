package kr.wonguni.nationwar.service;

import java.util.*;
import java.util.Collection;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class FisherListener implements Listener {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final JobService jobs;
    private final Random rng = new Random();
    private final NamespacedKey gradeKey;

    private static final Set<Material> GRADE_TARGETS = EnumSet.of(
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH,
            Material.INK_SAC, Material.GLOW_INK_SAC,
            Material.KELP
    );

    public FisherListener(JavaPlugin plugin, DataStore store, JobService jobs) {
        this.plugin = plugin;
        this.store = store;
        this.jobs = jobs;
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
    }

    // Kelp ownership: track placed kelp by fisher (문서 §9.10)
    private String locKey(org.bukkit.block.Block b) {
        return b.getWorld().getUID().toString() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKelpPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.KELP_PLANT && e.getBlockPlaced().getType() != Material.KELP) return;
        if (!jobs.getJobs(e.getPlayer().getUniqueId()).contains(JobType.FISHER)) return;
        store.setCropOwner(locKey(e.getBlockPlaced()), e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKelpHarvest(org.bukkit.event.block.BlockBreakEvent e) {
        org.bukkit.block.Block b = e.getBlock();
        if (b.getType() != Material.KELP && b.getType() != Material.KELP_PLANT) return;
        Player p = e.getPlayer();
        if (!jobs.getJobs(p.getUniqueId()).contains(JobType.FISHER)) return;

        // Natural kelp or fisher-placed kelp: apply grade
        java.util.UUID owner = store.getCropOwner(locKey(b));
        boolean isNatural = (owner == null); // no owner = natural
        boolean isOwnFisher = (owner != null && owner.equals(p.getUniqueId()));

        if (isNatural || isOwnFisher) {
            Collection<ItemStack> drops = b.getDrops(p.getInventory().getItemInMainHand(), p);
            if (drops == null || drops.isEmpty()) return;
            e.setDropItems(false);

            PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
            int rank = Math.max(0, Math.min(7, prof.getJobRank(JobType.FISHER)));
            double rare = plugin.getConfig().getDouble("fisher.grade-chance-by-rank." + rank + ".rare", 0.0);
            double epic = plugin.getConfig().getDouble("fisher.grade-chance-by-rank." + rank + ".epic", 0.0);

            for (ItemStack it : drops) {
                if (it == null || it.getType() == Material.AIR) continue;
                if (it.getType() == Material.KELP) {
                    int amt = it.getAmount();
                    for (int i = 0; i < amt; i++) {
                        ItemStack one = it.clone();
                        one.setAmount(1);
                        int g = rollGrade(rare, epic);
                        if (g > 0) applyGrade(one, g);
                        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), one);
                    }
                } else {
                    b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), it);
                }
            }
        }
        // Non-fisher-placed kelp by another player: vanilla drops
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(e.getCaught() instanceof Item itemEnt)) return;
        Player p = e.getPlayer();

        if (!jobs.getJobs(p.getUniqueId()).contains(JobType.FISHER)) return;

        PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
        int rank = Math.max(0, Math.min(7, prof.getJobRank(JobType.FISHER)));

        // Proficiency gain: fishing grants XP
        int xpGain = plugin.getConfig().getInt("jobs.proficiency.xp-per-action.fisher", 1);
        int oldLevel = prof.getJobProficiencyLevel(JobType.FISHER);
        prof.addJobProficiency(JobType.FISHER, xpGain);
        int newLevel = prof.getJobProficiencyLevel(JobType.FISHER);
        if (newLevel > oldLevel) {
            p.sendMessage("§e[어부] §f숙련도 레벨 UP! §a" + oldLevel + " → " + newLevel);
        }
        store.savePlayers();

        double dbl = plugin.getConfig().getDouble("fisher.double-chance-by-rank." + rank, 0.0);
        boolean doubled = rng.nextDouble() < dbl;

        ItemStack caught = itemEnt.getItemStack();

        // Enchant book boost (+1~+8), exclude single-level enchants
        if (caught.getType() == Material.ENCHANTED_BOOK && caught.getItemMeta() instanceof EnchantmentStorageMeta esm) {
            int boost = plugin.getConfig().getInt("fisher.enchant-boost-by-rank." + rank, 1);
            Map<Enchantment, Integer> stored = new HashMap<>(esm.getStoredEnchants());
            for (Enchantment en : new ArrayList<>(stored.keySet())) esm.removeStoredEnchant(en);
            for (Map.Entry<Enchantment, Integer> en : stored.entrySet()) {
                int lvl = en.getValue() == null ? 0 : en.getValue();
                if (lvl <= 1) esm.addStoredEnchant(en.getKey(), lvl, true);
                else esm.addStoredEnchant(en.getKey(), lvl + boost, true);
            }
            caught.setItemMeta(esm);
        }

        // Grade rolling: per item unit. Doubling creates more rolls.
        if (GRADE_TARGETS.contains(caught.getType())) {
            int amt = caught.getAmount();
            List<ItemStack> out = new ArrayList<>();
            double rare = plugin.getConfig().getDouble("fisher.grade-chance-by-rank." + rank + ".rare", 0.0);
            double epic = plugin.getConfig().getDouble("fisher.grade-chance-by-rank." + rank + ".epic", 0.0);

            int totalAmt = doubled ? (amt * 2) : amt;
            for (int i = 0; i < totalAmt; i++) {
                ItemStack one = caught.clone();
                one.setAmount(1);
                int g = rollGrade(rare, epic);
                if (g > 0) applyGrade(one, g);
                out.add(one);
            }
            itemEnt.remove();
            for (ItemStack it : out) p.getWorld().dropItemNaturally(p.getLocation(), it);
            return;
        }

        // Default doubling
        if (doubled) {
            if (caught.getMaxStackSize() > 1) {
                caught.setAmount(caught.getAmount() * 2);
                itemEnt.setItemStack(caught);
            } else {
                p.getWorld().dropItemNaturally(itemEnt.getLocation(), caught.clone());
            }
        }
    }

    private int rollGrade(double rare, double epic) {
        double r = rng.nextDouble();
        if (r < epic) return 2;
        if (r < epic + rare) return 1;
        return 0;
    }

    private void applyGrade(ItemStack item, int grade) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(gradeKey, PersistentDataType.INTEGER, grade);
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        if (grade == 1) lore.add(0, "§6[고급]");
        if (grade == 2) lore.add(0, "§d[최고급]");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
