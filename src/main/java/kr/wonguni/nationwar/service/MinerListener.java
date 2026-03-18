package kr.wonguni.nationwar.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.RawOreType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MinerListener implements Listener {
    private final RawOreService rawOres;
    private final JavaPlugin plugin;
    private final DataStore store;
    private final JobService jobService;
    private final NamespacedKey gradeKey;
    private final Random rng = new Random();

    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.AMETHYST_CLUSTER,
            Material.ANCIENT_DEBRIS
    );

    public MinerListener(JavaPlugin plugin, DataStore store, JobService jobService) {
        this.plugin = plugin;
        this.store = store;
        this.jobService = jobService;
        this.rawOres = new RawOreService(plugin);
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");

        // Miner passive: Haste I~VIII while holding a pickaxe
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!jobService.getJobs(p.getUniqueId()).contains(JobType.MINER)) continue;
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) continue;
                if (!hand.getType().name().endsWith("_PICKAXE")) continue;
                PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
                int rank = clampRank(prof.getJobRank(JobType.MINER));
                // amplifier 0 = Haste I
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, rank, true, false, false));
            }
        }, 20L, 20L);
    }

    private int clampRank(int r) {
        if (r < 0) return 0;
        if (r > 7) return 7;
        return r;
    }

    private String locKey(Block b) {
        return b.getWorld().getUID().toString() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!ORE_BLOCKS.contains(b.getType())) return;
        store.markPlacedOre(locKey(b));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (!ORE_BLOCKS.contains(b.getType())) return;
        if (!jobService.getJobs(p.getUniqueId()).contains(JobType.MINER)) return;

        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            String k = locKey(b);
            if (store.isPlacedOre(k)) store.unmarkPlacedOre(k);
            return;
        }

        String k = locKey(b);
        if (store.isPlacedOre(k)) {
            store.unmarkPlacedOre(k);
            return;
        }

        Collection<ItemStack> drops = b.getDrops(tool, p);
        if (drops == null || drops.isEmpty()) return;
        e.setDropItems(false);

        PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
        int rank = clampRank(prof.getJobRank(JobType.MINER));
        double rare = plugin.getConfig().getDouble("miner.grade-chance-by-rank." + rank + ".rare", 0.0);
        double epic = plugin.getConfig().getDouble("miner.grade-chance-by-rank." + rank + ".epic", 0.0);

        for (ItemStack it : drops) {
            RawOreType rt = rawTypeFromDrop(it);
            if (rt != null) {
                int amt = it.getAmount();
                for (int i = 0; i < amt; i++) {
                    int g = rollGrade(rare, epic);
                    ItemStack one = this.rawOres.rawOre(rt, g, 1);
                    b.getWorld().dropItemNaturally(b.getLocation().add(0.5,0.5,0.5), one);
                }
                continue;
            }
            if (it == null || it.getType() == Material.AIR) continue;
            int amt = it.getAmount();
            for (int i = 0; i < amt; i++) {
                ItemStack one = it.clone();
                one.setAmount(1);
                int g = rollGrade(rare, epic);
                if (g > 0) applyGrade(one, g);
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), one);
            }
        }
    }

    private int rollGrade(double rare, double epic) {
        double r = rng.nextDouble();
        if (r < epic) return 2;
        if (r < epic + rare) return 1;
        return 0;
    }

    

private RawOreType rawTypeFromDrop(ItemStack it) {
    if (it == null) return null;
    switch (it.getType()) {
        case DIAMOND: return RawOreType.DIAMOND;
        case EMERALD: return RawOreType.EMERALD;
        case REDSTONE: return RawOreType.REDSTONE;
        case LAPIS_LAZULI: return RawOreType.LAPIS;
        case QUARTZ: return RawOreType.QUARTZ;
        case AMETHYST_SHARD: return RawOreType.AMETHYST;
        case ANCIENT_DEBRIS: return RawOreType.ANCIENT_DEBRIS;
        default: return null;
    }
}

private void applyGrade(ItemStack item, int grade) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(gradeKey, PersistentDataType.INTEGER, grade);

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<String>();
        if (grade == 1) lore.add(0, "§6[고급]");
        if (grade == 2) lore.add(0, "§d[최고급]");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent e) {
        ItemStack src = e.getSource();
        if (src == null) return;
        ItemMeta sm = src.getItemMeta();
        if (sm == null) return;
        Integer g = sm.getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
        if (g == null || g.intValue() <= 0) return;

        ItemStack res = e.getResult();
        if (res == null) return;
        ItemMeta rm = res.getItemMeta();
        if (rm == null) return;
        rm.getPersistentDataContainer().set(gradeKey, PersistentDataType.INTEGER, g);
        res.setItemMeta(rm);
        e.setResult(res);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (!(e.getInventory() instanceof CraftingInventory)) return;
        ItemStack[] matrix = ((CraftingInventory)e.getInventory()).getMatrix();
        for (ItemStack it : matrix) {
            if (hasGrade(it)) {
                ((CraftingInventory)e.getInventory()).setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareSmith(PrepareSmithingEvent e) {
        if (!(e.getInventory() instanceof SmithingInventory)) return;
        SmithingInventory inv = (SmithingInventory)e.getInventory();
        ItemStack templ = inv.getInputTemplate();
        ItemStack base = inv.getInputEquipment();
        ItemStack add = inv.getInputMineral();
        if (hasGrade(templ) || hasGrade(base) || hasGrade(add)) {
            inv.setResult(null);
        }
    }

    private boolean hasGrade(ItemStack it) {
        if (it == null) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        Integer g = m.getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
        return g != null && g.intValue() > 0;
    }
}
