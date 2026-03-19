package kr.wonguni.nationwar.service;

import java.util.*;
import java.util.Collections;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class HunterListener implements Listener {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final JobService jobs;
    private final Random rng = new Random();

    private final NamespacedKey gradeKey;
    private final NamespacedKey headKey;
    private final NamespacedKey headTierKey;

    private static final Set<EntityType> EXTRA_HOSTILE = EnumSet.of(EntityType.POLAR_BEAR, EntityType.ZOMBIFIED_PIGLIN);
    private static final Set<EntityType> EXCLUDED = EnumSet.of(EntityType.ENDER_DRAGON, EntityType.SILVERFISH, EntityType.PUFFERFISH);

    private static final Set<Material> GRADE_LOOT = EnumSet.of(
            Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.MUTTON, Material.RABBIT,
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
    );

    public HunterListener(JavaPlugin plugin, DataStore store, JobService jobs) {
        this.plugin = plugin;
        this.store = store;
        this.jobs = jobs;
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
        this.headKey = new NamespacedKey(plugin, "nw_hunter_head");
        this.headTierKey = new NamespacedKey(plugin, "nw_head_tier");
    }

    // Whitelist: specific mobs drop specific items with grade
    private static final Map<EntityType, Set<Material>> WHITELIST_LOOT = new HashMap<>();
    static {
        Set<Material> zombieLoot = EnumSet.of(Material.IRON_INGOT, Material.CARROT, Material.POTATO);
        WHITELIST_LOOT.put(EntityType.ZOMBIE, zombieLoot);
        WHITELIST_LOOT.put(EntityType.HUSK, zombieLoot);
        WHITELIST_LOOT.put(EntityType.ZOMBIE_VILLAGER, zombieLoot);
        WHITELIST_LOOT.put(EntityType.DROWNED, EnumSet.of(Material.COPPER_INGOT));
        WHITELIST_LOOT.put(EntityType.ZOMBIFIED_PIGLIN, EnumSet.of(Material.GOLD_NUGGET, Material.GOLD_INGOT));
        WHITELIST_LOOT.put(EntityType.VINDICATOR, EnumSet.of(Material.EMERALD));
        WHITELIST_LOOT.put(EntityType.EVOKER, EnumSet.of(Material.EMERALD));
        WHITELIST_LOOT.put(EntityType.WITCH, EnumSet.of(Material.REDSTONE));
        WHITELIST_LOOT.put(EntityType.SQUID, EnumSet.of(Material.INK_SAC));
        WHITELIST_LOOT.put(EntityType.GLOW_SQUID, EnumSet.of(Material.GLOW_INK_SAC));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        boolean isHunter = jobs.getJobs(killer.getUniqueId()).contains(JobType.HUNTER);
        PlayerProfile prof = store.getOrCreatePlayer(killer.getUniqueId());
        int rank = Math.max(0, Math.min(7, prof.getJobRank(JobType.HUNTER)));

        // Proficiency gain: mob kill grants XP
        if (isHunter) {
            int xpGain = plugin.getConfig().getInt("jobs.proficiency.xp-per-action.hunter", 1);
            int oldLevel = prof.getJobProficiencyLevel(JobType.HUNTER);
            prof.addJobProficiency(JobType.HUNTER, xpGain);
            int newLevel = prof.getJobProficiencyLevel(JobType.HUNTER);
            if (newLevel > oldLevel) {
                killer.sendMessage("§e[사냥꾼] §f숙련도 레벨 UP! §a" + oldLevel + " → " + newLevel);
            }
            store.savePlayers();
        }

        if (isHunter) {
            double rare = plugin.getConfig().getDouble("hunter.loot-grade-chance-by-rank." + rank + ".rare", 0.0);
            double epic = plugin.getConfig().getDouble("hunter.loot-grade-chance-by-rank." + rank + ".epic", 0.0);

            // Determine which items can be graded (food + whitelist)
            Set<Material> whitelistMats = WHITELIST_LOOT.getOrDefault(e.getEntity().getType(), Collections.emptySet());

            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack it : e.getDrops()) {
                if (it == null || it.getType() == Material.AIR) continue;
                if (!GRADE_LOOT.contains(it.getType()) && !whitelistMats.contains(it.getType())) {
                    newDrops.add(it);
                    continue;
                }
                int amt = it.getAmount();
                for (int i = 0; i < amt; i++) {
                    ItemStack one = it.clone();
                    one.setAmount(1);
                    int g = rollGrade(rare, epic);
                    if (g > 0) applyGrade(one, g);
                    newDrops.add(one);
                }
            }
            e.getDrops().clear();
            e.getDrops().addAll(newDrops);
        }

        if (!isHeadEligible(e.getEntity())) return;
        if (isIndirectOneShot20Plus(e.getEntity())) return;

        double chance = isHunter
                ? plugin.getConfig().getDouble("hunter.head-drop-chance-by-rank." + rank, 0.10)
                : plugin.getConfig().getDouble("hunter.head-drop-chance-by-rank.non_hunter", 0.01);

        if (rng.nextDouble() > chance) return;

        int tier = tierOf(e.getEntity().getType());
        int lvl = mobLevel(e.getEntity());
        int headGrade = rollHeadGradeByLevel(lvl);
        ItemStack head = createHeadItem(e.getEntity().getType(), tier, headGrade, lvl);
        e.getDrops().add(head);
    }

    private boolean isHeadEligible(Entity entity) {
        if (entity == null) return false;
        EntityType t = entity.getType();
        if (EXCLUDED.contains(t)) return false;
        if (entity instanceof org.bukkit.entity.Monster) return true;
        return EXTRA_HOSTILE.contains(t);
    }

    private boolean isIndirectOneShot20Plus(Entity entity) {
        EntityDamageEvent cause = entity.getLastDamageCause();
        if (cause == null) return false;
        // Block heads if: non-player-attack damage type OR single hit >= 20
        EntityDamageEvent.DamageCause dc = cause.getCause();
        boolean isPlayerAttack = (dc == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || dc == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                || dc == EntityDamageEvent.DamageCause.PROJECTILE);
        if (!isPlayerAttack) return true; // indirect kill → no head
        return cause.getFinalDamage() >= 20.0;
    }

    private int mobLevel(Entity entity) {
        double x = Math.abs(entity.getLocation().getX());
        double z = Math.abs(entity.getLocation().getZ());
        double d = Math.max(x, z);
        int lvl = (int)(d / 100.0) + 1;
        if (lvl < 1) lvl = 1;
        if (lvl > 100) lvl = 100;
        return lvl;
    }

    private int rollHeadGradeByLevel(int lvl) {
        double v1   = plugin.getConfig().getDouble("hunter.head-grade-by-level.1.vanilla", 0.99);
        double r1   = plugin.getConfig().getDouble("hunter.head-grade-by-level.1.rare", 0.01);
        double e1   = plugin.getConfig().getDouble("hunter.head-grade-by-level.1.epic", 0.0);
        double v50  = plugin.getConfig().getDouble("hunter.head-grade-by-level.50.vanilla", 0.30);
        double r50  = plugin.getConfig().getDouble("hunter.head-grade-by-level.50.rare", 0.65);
        double e50  = plugin.getConfig().getDouble("hunter.head-grade-by-level.50.epic", 0.05);
        double v100 = plugin.getConfig().getDouble("hunter.head-grade-by-level.100.vanilla", 0.20);
        double r100 = plugin.getConfig().getDouble("hunter.head-grade-by-level.100.rare", 0.40);
        double e100 = plugin.getConfig().getDouble("hunter.head-grade-by-level.100.epic", 0.40);

        double v, r, e;
        if (lvl <= 50) {
            double t = (lvl - 1) / 49.0;
            v = lerp(v1, v50, t);
            r = lerp(r1, r50, t);
            e = lerp(e1, e50, t);
        } else {
            double t = (lvl - 50) / 50.0;
            v = lerp(v50, v100, t);
            r = lerp(r50, r100, t);
            e = lerp(e50, e100, t);
        }

        double sum = v + r + e;
        if (sum <= 0) { v = 1; r = 0; e = 0; sum = 1; }
        v /= sum; r /= sum; e /= sum;

        double roll = rng.nextDouble();
        if (roll < e) return 2;
        if (roll < e + r) return 1;
        return 0;
    }

    private double lerp(double a, double b, double t) {
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return a + (b - a) * t;
    }

    private int tierOf(EntityType type) {
        switch (type) {
            case ENDERMAN:
            case WITCH:
            case BLAZE:
            case GHAST:
            case VINDICATOR:
            case BREEZE:
            case PIGLIN_BRUTE:
            case SHULKER:
                return 2;
            case WARDEN:
            case EVOKER:
            case WITHER_SKELETON:
            case ELDER_GUARDIAN:
            case RAVAGER:
                return 3;
            default:
                return 1;
        }
    }

    private ItemStack createHeadItem(EntityType type, int tier, int grade, int lvl) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§f몹 머리 §7(" + type.name() + ")");
        List<String> lore = new ArrayList<>();
        lore.add("§7티어: §fT" + tier);
        lore.add("§7레벨: §f" + lvl);
        if (grade == 1) lore.add("§6[고급]");
        if (grade == 2) lore.add("§d[최고급]");
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(headKey, PersistentDataType.INTEGER, 1);
        pdc.set(headTierKey, PersistentDataType.INTEGER, tier);
        if (grade > 0) pdc.set(gradeKey, PersistentDataType.INTEGER, grade);

        item.setItemMeta(meta);
        return item;
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
