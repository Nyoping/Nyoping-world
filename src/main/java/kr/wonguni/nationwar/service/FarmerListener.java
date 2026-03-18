package kr.wonguni.nationwar.service;

import java.util.*;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.CustomCropType;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class FarmerListener implements Listener {
    private final CustomCropService customCrops;
    private final JavaPlugin plugin;
    private final DataStore store;
    private final JobService jobs;
    private final Random rng = new Random();
    private final NamespacedKey gradeKey;

    private static final Set<Material> OWNED_PLANT_BLOCKS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.COCOA,
            Material.MELON_STEM, Material.PUMPKIN_STEM,
            Material.ATTACHED_MELON_STEM, Material.ATTACHED_PUMPKIN_STEM
    );

    public FarmerListener(JavaPlugin plugin, DataStore store, JobService jobs, CustomCropService customCrops) {
        this.plugin = plugin;
        this.store = store;
        this.jobs = jobs;
        this.customCrops = customCrops;
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
    }

    private String locKey(Block b) {
        return b.getWorld().getUID().toString() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!OWNED_PLANT_BLOCKS.contains(b.getType())) return;
        store.setCropOwner(locKey(b), e.getPlayer().getUniqueId());
    }

    // Farmland trampling protection (문서: 밟힘 방지)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTrample(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.FARMLAND) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTrample(EntityChangeBlockEvent e) {
        if (e.getBlock() == null) return;
        if (e.getBlock().getType() != Material.FARMLAND) return;
        // prevent farmland from turning to dirt
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Material type = b.getType();

        // Only farmers get bonuses
        if (!jobs.getJobs(p.getUniqueId()).contains(JobType.FARMER)) return;

        // Handle melon/pumpkin blocks via adjacent stem ownership
        if (type == Material.MELON || type == Material.PUMPKIN) {
            if (!isOwnedByAdjacentStem(p, b)) return;
            handleDropsWithBonuses(e, p, b, null, false);
            return;
        }

        BlockData data = b.getBlockData();
        if (!(data instanceof Ageable)) return;
        Ageable age = (Ageable)data;
        if (age.getAge() < age.getMaximumAge()) return; // only mature

        // ownership condition: same player placed AND harvest
        java.util.UUID owner = store.getCropOwner(locKey(b));
        if (owner == null || !owner.equals(p.getUniqueId())) return;

        // replant: preserve block data (direction/facing) but set age=0
        BlockData originalData = b.getBlockData().clone();
        handleDropsWithBonuses(e, p, b, originalData, true);
    }

    private boolean isOwnedByAdjacentStem(Player p, Block block) {
        int[][] dirs = new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
        for (int[] d : dirs) {
            Block adj = block.getRelative(d[0], d[1], d[2]);
            Material t = adj.getType();
            if (t == Material.ATTACHED_MELON_STEM || t == Material.ATTACHED_PUMPKIN_STEM || t == Material.MELON_STEM || t == Material.PUMPKIN_STEM) {
                java.util.UUID owner = store.getCropOwner(locKey(adj));
                if (owner != null && owner.equals(p.getUniqueId())) return true;
            }
        }
        return false;
    }

    private void handleDropsWithBonuses(BlockBreakEvent e, Player p, Block b, BlockData replantData, boolean doReplant) {
        Collection<ItemStack> drops = b.getDrops(p.getInventory().getItemInMainHand(), p);
        if (drops == null || drops.isEmpty()) return;
        e.setDropItems(false);

        PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
        int rank = Math.max(0, Math.min(7, prof.getJobRank(JobType.FARMER)));

        double rare = plugin.getConfig().getDouble("farmer.grade-chance-by-rank." + rank + ".rare", 0.0);
        double epic = plugin.getConfig().getDouble("farmer.grade-chance-by-rank." + rank + ".epic", 0.0);
        double dbl = plugin.getConfig().getDouble("farmer.double-chance-by-rank." + rank, 0.0);

        boolean doubled = rng.nextDouble() < dbl;

        for (ItemStack it : drops) {
            if (it == null || it.getType() == Material.AIR) continue;
            int amt = it.getAmount();
            if (doubled) amt *= 2;
            for (int i = 0; i < amt; i++) {
                ItemStack one = it.clone();
                one.setAmount(1);
                int g = rollGrade(rare, epic);
                if (g > 0) applyGrade(one, g);
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), one);
            }
        }

        if (doReplant && replantData != null) {
            // try consume seed/plant item; if not available, do nothing (문서: 없으면 실패)
            Material seed = seedFor(b.getType());
            if (seed != null && consumeOne(p, seed)) {
                // restore same crop with age 0, preserving direction/facing
                b.setType(b.getType(), false);
                BlockData bd = replantData.clone();
                if (bd instanceof Ageable) {
                    ((Ageable)bd).setAge(0);
                }
                b.setBlockData(bd, false);
                store.setCropOwner(locKey(b), p.getUniqueId()); // keep ownership
            } else {
                store.clearCropOwner(locKey(b)); // ownership breaks if not replanted
            }
        }
    }

    private Material seedFor(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT_SEEDS;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT_SEEDS;
            case NETHER_WART: return Material.NETHER_WART;
            case COCOA: return Material.COCOA_BEANS;
            default: return null;
        }
    }

    

private boolean consumeOneCustomSeed(Player p, CustomCropType type) {
    if (type == null) return false;
    ItemStack[] contents = p.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
        ItemStack it = contents[i];
        if (it == null || it.getType() != Material.WHEAT_SEEDS) continue;
        if (this.customCrops.seedType(it) != type) continue;
        int a = it.getAmount();
        if (a <= 1) contents[i] = null;
        else it.setAmount(a - 1);
        p.getInventory().setContents(contents);
        p.updateInventory();
        return true;
    }
    return false;
}

private boolean consumeOne(Player p, Material mat) {
        if (mat == null) return false;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != mat) continue;
            int a = it.getAmount();
            if (a <= 1) contents[i] = null;
            else it.setAmount(a - 1);
            p.getInventory().setContents(contents);
            p.updateInventory();
            return true;
        }
        return false;
    }

    private int rollGrade(double rare, double epic) {
        double r = rng.nextDouble();
        if (r < epic) return 2;
        if (r < epic + rare) return 1;
        return 0;
    }

    

private Collection<ItemStack> createCustomWheatDrops(CustomCropType type) {
    java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>();
    // mimic wheat yield: 1 produce + 0~3 seeds
    out.add(this.customCrops.cropItem(type, 1));
    int seeds = this.rng.nextInt(4); // 0..3
    if (seeds > 0) out.add(this.customCrops.seedItem(type, seeds));
    return out;
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
