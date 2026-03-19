package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.CustomFoodType;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Cook recipe trigger (문서 §11 전체 레시피).
 * 조리시간 감소 특전, 등급 판정 타이밍, 숙련도, 가마솥 저장형, 용기 반환 포함.
 */
public class CookTriggerListener implements Listener {
    private final JavaPlugin plugin;
    private final JobService jobs;
    private final DataStore store;
    private final CustomFoodService foods;
    private final Random rng = new Random();

    private final NamespacedKey itemIdKey;
    private final NamespacedKey gradeKey;
    private final NamespacedKey cookRecipeKey;
    private final NamespacedKey cookChargesKey;

    // Cook time reduction by rank (문서 §11.9)
    private static final double[] TIME_REDUCTION = {0.0, 0.0, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30};

    public CookTriggerListener(JavaPlugin plugin, JobService jobs, DataStore store, CustomFoodService foods) {
        this.plugin = plugin;
        this.jobs = jobs;
        this.store = store;
        this.foods = foods;
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
        this.cookRecipeKey = new NamespacedKey(plugin, "nw_cook_recipe");
        this.cookChargesKey = new NamespacedKey(plugin, "nw_cook_charges");
    }

    private boolean hasCook(Player p) {
        return p != null && jobs.getJobs(p.getUniqueId()).contains(JobType.COOK);
    }

    private boolean isLadle(ItemStack it) { return matchesTool(it, "cook_ladle", "국자"); }
    private boolean isSpatula(ItemStack it) { return matchesTool(it, "cook_spatula", "뒤집개"); }
    private boolean isKnife(ItemStack it) { return matchesTool(it, "cook_knife", "식칼"); }

    private boolean matchesTool(ItemStack it, String id, String nameContains) {
        if (it == null || it.getType() == Material.AIR) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        String pid = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (pid != null && pid.equalsIgnoreCase(id)) return true;
        if (meta.hasDisplayName()) {
            String dn = meta.getDisplayName();
            return dn != null && dn.contains(nameContains);
        }
        return false;
    }

    private boolean isLitCampfireBelow(Block b) {
        if (b == null) return false;
        Block below = b.getRelative(0, -1, 0);
        if (below.getType() != Material.CAMPFIRE && below.getType() != Material.SOUL_CAMPFIRE) return false;
        if (!(below.getBlockData() instanceof Campfire c)) return false;
        return c.isLit();
    }

    private String getItemId(ItemStack it) {
        if (it == null) return null;
        ItemMeta m = it.getItemMeta();
        if (m == null) return null;
        return m.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private boolean hasItem(Player p, Material mat) {
        return p.getInventory().contains(mat);
    }

    private boolean hasCustomItem(Player p, String itemId) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null) continue;
            String id = getItemId(it);
            if (itemId.equals(id)) return true;
        }
        return false;
    }

    private boolean removeOne(Player p, Material mat) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != mat) continue;
            // Block graded items from recipes
            if (hasGrade(it)) continue;
            int a = it.getAmount();
            if (a <= 1) p.getInventory().setItem(i, null);
            else it.setAmount(a - 1);
            return true;
        }
        return false;
    }

    private boolean removeCustom(Player p, String itemId) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null) continue;
            String id = getItemId(it);
            if (!itemId.equals(id)) continue;
            if (hasGrade(it)) continue;
            int a = it.getAmount();
            if (a <= 1) p.getInventory().setItem(i, null);
            else it.setAmount(a - 1);
            return true;
        }
        return false;
    }

    private boolean hasGrade(ItemStack it) {
        if (it == null) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        Integer g = m.getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
        return g != null && g > 0;
    }

    private void giveItem(Player p, ItemStack item) {
        if (!p.getInventory().addItem(item).isEmpty()) {
            p.getWorld().dropItemNaturally(p.getLocation(), item);
        }
    }

    // --- Cauldron state helpers ---
    private PersistentDataContainer pdc(Block tile) {
        if (!(tile.getState() instanceof TileState ts)) return null;
        return ts.getPersistentDataContainer();
    }

    private int getCharges(Block b) {
        PersistentDataContainer pdc = pdc(b);
        if (pdc == null) return 0;
        Integer v = pdc.get(cookChargesKey, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    private String getRecipe(Block b) {
        PersistentDataContainer pdc = pdc(b);
        if (pdc == null) return null;
        return pdc.get(cookRecipeKey, PersistentDataType.STRING);
    }

    private void setCauldronState(Block b, String recipeId, int charges) {
        if (!(b.getState() instanceof TileState ts)) return;
        PersistentDataContainer pdc = ts.getPersistentDataContainer();
        if (recipeId == null || charges <= 0) {
            pdc.remove(cookRecipeKey);
            pdc.remove(cookChargesKey);
        } else {
            pdc.set(cookRecipeKey, PersistentDataType.STRING, recipeId);
            pdc.set(cookChargesKey, PersistentDataType.INTEGER, charges);
        }
        ts.update(true, false);
    }

    // --- Grade rolling on completion (문서 §11.10) ---
    private int rollGrade(int rank) {
        double rare = plugin.getConfig().getDouble("cook.grade-chance-by-rank." + rank + ".rare", 0.0);
        double epic = plugin.getConfig().getDouble("cook.grade-chance-by-rank." + rank + ".epic", 0.0);
        double r = rng.nextDouble();
        if (r < epic) return 2;
        if (r < epic + rare) return 1;
        return 0;
    }

    private void applyGrade(ItemStack item, int grade) {
        if (grade <= 0) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(gradeKey, PersistentDataType.INTEGER, grade);
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        if (grade == 1) lore.add(0, "§6[고급]");
        if (grade == 2) lore.add(0, "§d[최고급]");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /** Snapshot rank at start, grade at completion. */
    private void cookWithDelay(Player p, int rank, long baseTicks, CustomFoodType result, int amount, boolean reducible, Runnable onComplete) {
        long ticks = baseTicks;
        if (reducible && rank < TIME_REDUCTION.length) {
            ticks = (long)(baseTicks * (1.0 - TIME_REDUCTION[rank]));
        }
        if (ticks < 1) ticks = 1;

        p.sendMessage("§e[요리] §f조리 시작... (" + String.format("%.1f", ticks / 20.0) + "초)");

        final int snapRank = rank;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Proficiency gain
            PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
            int xpGain = plugin.getConfig().getInt("jobs.proficiency.xp-per-action.cook", 1);
            int oldLevel = prof.getJobProficiencyLevel(JobType.COOK);
            prof.addJobProficiency(JobType.COOK, xpGain);
            int newLevel = prof.getJobProficiencyLevel(JobType.COOK);
            if (newLevel > oldLevel) {
                p.sendMessage("§e[요리사] §f숙련도 레벨 UP! §a" + oldLevel + " → " + newLevel);
            }
            store.savePlayers();

            // Grade judgment at completion for final dishes
            for (int i = 0; i < amount; i++) {
                ItemStack out = foods.foodItem(result, 1);
                if (result.isFinalDish()) {
                    int g = rollGrade(snapRank);
                    applyGrade(out, g);
                }
                giveItem(p, out);
            }

            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
            p.sendMessage("§a[요리] §f조리 완료: §f" + result.koName() + (amount > 1 ? " ×" + amount : ""));

            if (onComplete != null) onComplete.run();
        }, ticks);
    }

    // --- Container return helper ---
    private void returnContainer(Player p, Material container) {
        giveItem(p, new ItemStack(container, 1));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        ItemStack hand = p.getInventory().getItemInMainHand();

        // === 가마솥 서빙 (그릇 우클릭) ===
        if ((b.getType() == Material.CAULDRON || b.getType() == Material.WATER_CAULDRON) && hand.getType() == Material.BOWL) {
            int charges = getCharges(b);
            String recipe = getRecipe(b);
            if (charges <= 0 || recipe == null) return;

            CustomFoodType food = CustomFoodType.fromId(recipe);
            if (food == null) return;

            e.setCancelled(true);
            ItemStack out = foods.foodItem(food, 1);
            // Grade already set at batch creation (stored in recipe state)
            giveItem(p, out);

            charges -= 1;
            setCauldronState(b, recipe, charges);

            hand.setAmount(hand.getAmount() - 1); // consume bowl
            p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.8f, 1.3f);
            p.sendMessage("§a[요리] §f서빙 완료. 남은 서빙: §e" + Math.max(0, charges));
            return;
        }

        if (!hasCook(p)) return;

        PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
        int rank = Math.max(0, Math.min(7, prof.getJobRank(JobType.COOK)));

        // === 도마 (BARREL + 식칼) ===
        if (b.getType() == Material.BARREL && isKnife(hand)) {
            e.setCancelled(true);
            handleCuttingBoard(p, rank);
            return;
        }

        // === 프라이팬 (STONECUTTER + 뒤집개) ===
        if (b.getType() == Material.STONECUTTER && isSpatula(hand)) {
            e.setCancelled(true);
            if (!isLitCampfireBelow(b)) {
                p.sendMessage("§c[요리] §f프라이팬은 불 켜진 모닥불 위에서만 조리가 가능합니다.");
                return;
            }
            handleFryingPan(p, rank);
            return;
        }

        // === 가마솥 (CAULDRON/WATER_CAULDRON + 국자) ===
        if ((b.getType() == Material.CAULDRON || b.getType() == Material.WATER_CAULDRON) && isLadle(hand)) {
            e.setCancelled(true);
            if (!isLitCampfireBelow(b)) {
                p.sendMessage("§c[요리] §f가마솥은 불 켜진 모닥불 위에서만 조리가 가능합니다.");
                return;
            }
            if (b.getType() != Material.WATER_CAULDRON) {
                p.sendMessage("§c[요리] §f가마솥에 물을 먼저 채워주세요.");
                return;
            }
            if (getCharges(b) > 0) {
                p.sendMessage("§e[요리] §f이미 서빙이 남아있습니다. 그릇으로 서빙하세요.");
                return;
            }
            handleCauldron(p, rank, b);
            return;
        }
    }

    // === A. 도마 레시피 (식칼 우클릭, 시간단축 적용) ===
    private void handleCuttingBoard(Player p, int rank) {
        // 고추 → 고춧가루×4 (0.6s = 12t)
        if (removeCustom(p, "crop_CHILI") || removeCustom(p, "food_" + CustomFoodType.CHILI_POWDER.name().toLowerCase())) {
            // re-check: need raw chili
        }
        if (hasCustomItem(p, "crop_CHILI") && removeCustom(p, "crop_CHILI")) {
            cookWithDelay(p, rank, 12, CustomFoodType.CHILI_POWDER, 4, true, null);
            return;
        }
        // 연어 → 연어회 (1.2s = 24t)
        if (removeOne(p, Material.SALMON)) {
            cookWithDelay(p, rank, 24, CustomFoodType.SALMON_SASHIMI, 1, true, null);
            return;
        }
        // 대구 → 대구회 (1.2s = 24t)
        if (removeOne(p, Material.COD)) {
            cookWithDelay(p, rank, 24, CustomFoodType.COD_SASHIMI, 1, true, null);
            return;
        }
        // 복어 → 복어회 (1.8s = 36t)
        if (removeOne(p, Material.PUFFERFISH)) {
            cookWithDelay(p, rank, 36, CustomFoodType.PUFFERFISH_SASHIMI, 1, true, null);
            return;
        }
        // 감자 → 썰린 감자 (0.8s = 16t)
        if (removeOne(p, Material.POTATO)) {
            cookWithDelay(p, rank, 16, CustomFoodType.SLICED_POTATO, 1, true, null);
            return;
        }
        p.sendMessage("§c[요리] §f도마에 올릴 재료가 없습니다.");
    }

    // === B. 프라이팬 레시피 (뒤집개 우클릭, 시간단축 적용) ===
    private void handleFryingPan(Player p, int rank) {
        // 계란 + 소금 → 계란후라이 (4s = 80t)
        if (hasItem(p, Material.EGG) && hasCustomItem(p, "food_SALT")) {
            if (removeOne(p, Material.EGG) && removeCustom(p, "food_SALT")) {
                cookWithDelay(p, rank, 80, CustomFoodType.EGG_FRY, 1, true, null);
                return;
            }
        }
        // 당근 → 구운 당근 (5s = 100t)
        if (removeOne(p, Material.CARROT)) {
            cookWithDelay(p, rank, 100, CustomFoodType.GRILLED_CARROT, 1, true, null);
            return;
        }
        // 보리 → 맥아 (6s = 120t)
        if (hasCustomItem(p, "crop_BARLEY") && removeCustom(p, "crop_BARLEY")) {
            cookWithDelay(p, rank, 120, CustomFoodType.MALT, 1, true, null);
            return;
        }
        // 스테이크 정식 (12s = 240t)
        if (hasCustomItem(p, "food_BUTTER") && hasItem(p, Material.BEEF) && hasItem(p, Material.BROWN_MUSHROOM)
                && hasCustomItem(p, "crop_GARLIC") && hasItem(p, Material.BREAD) && hasCustomItem(p, "food_SALT")) {
            if (removeCustom(p, "food_BUTTER") && removeOne(p, Material.BEEF) && removeOne(p, Material.BROWN_MUSHROOM)
                    && removeCustom(p, "crop_GARLIC") && removeOne(p, Material.BREAD) && removeCustom(p, "food_SALT")) {
                cookWithDelay(p, rank, 240, CustomFoodType.STEAK_SET, 1, true, null);
                return;
            }
        }
        // 제육볶음 (10s = 200t) → 빈 그릇 반환
        if (hasItem(p, Material.PORKCHOP) && hasCustomItem(p, "crop_GREEN_ONION") && hasCustomItem(p, "crop_ONION")
                && hasCustomItem(p, "food_GOCHUJANG") && hasItem(p, Material.SUGAR) && hasCustomItem(p, "crop_GARLIC")) {
            if (removeOne(p, Material.PORKCHOP) && removeCustom(p, "crop_GREEN_ONION") && removeCustom(p, "crop_ONION")
                    && removeCustom(p, "food_GOCHUJANG") && removeOne(p, Material.SUGAR) && removeCustom(p, "crop_GARLIC")) {
                cookWithDelay(p, rank, 200, CustomFoodType.JEYUK, 1, true, () -> returnContainer(p, Material.BOWL));
                return;
            }
        }
        // 치킨너겟 (12s = 240t)
        if (hasCustomItem(p, "food_BUTTER") && hasItem(p, Material.CHICKEN) && hasCustomItem(p, "food_DOUGH")
                && hasItem(p, Material.EGG) && hasCustomItem(p, "food_SALT")) {
            if (removeCustom(p, "food_BUTTER") && removeOne(p, Material.CHICKEN) && removeCustom(p, "food_DOUGH")
                    && removeOne(p, Material.EGG) && removeCustom(p, "food_SALT")) {
                cookWithDelay(p, rank, 240, CustomFoodType.CHICKEN_NUGGET, 1, true, null);
                return;
            }
        }
        // 감자튀김 (9s = 180t)
        if (hasCustomItem(p, "food_SLICED_POTATO") && hasCustomItem(p, "food_SALT") && hasCustomItem(p, "food_BUTTER")) {
            if (removeCustom(p, "food_SLICED_POTATO") && removeCustom(p, "food_SALT") && removeCustom(p, "food_BUTTER")) {
                cookWithDelay(p, rank, 180, CustomFoodType.FRENCH_FRIES, 1, true, null);
                return;
            }
        }
        // 양꼬치 (9s = 180t)
        if (hasItem(p, Material.STICK) && hasItem(p, Material.MUTTON) && hasCustomItem(p, "food_SALT") && hasCustomItem(p, "food_CHILI_POWDER")) {
            // need 3 mutton
            int muttonCount = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it != null && it.getType() == Material.MUTTON && !hasGrade(it)) muttonCount += it.getAmount();
            }
            if (muttonCount >= 3) {
                removeOne(p, Material.STICK);
                for (int i = 0; i < 3; i++) removeOne(p, Material.MUTTON);
                removeCustom(p, "food_SALT");
                removeCustom(p, "food_CHILI_POWDER");
                cookWithDelay(p, rank, 180, CustomFoodType.YANGKKOCHI, 1, true, null);
                return;
            }
        }
        // 햄버거×2 (13s = 260t)
        if (hasItem(p, Material.BREAD) && hasItem(p, Material.PORKCHOP) && hasItem(p, Material.BEEF)
                && hasCustomItem(p, "crop_ONION") && hasCustomItem(p, "food_BUTTER") && hasCustomItem(p, "food_SALT")
                && hasCustomItem(p, "crop_TOMATO") && hasCustomItem(p, "crop_CABBAGE")) {
            if (removeOne(p, Material.BREAD) && removeOne(p, Material.PORKCHOP) && removeOne(p, Material.BEEF)
                    && removeCustom(p, "crop_ONION") && removeCustom(p, "food_BUTTER") && removeCustom(p, "food_SALT")
                    && removeCustom(p, "crop_TOMATO") && removeCustom(p, "crop_CABBAGE")) {
                cookWithDelay(p, rank, 260, CustomFoodType.HAMBURGER, 2, true, null);
                return;
            }
        }
        // 비어치킨 (14s = 280t)
        if (hasItem(p, Material.CHICKEN) && hasCustomItem(p, "food_BEER") && hasCustomItem(p, "food_BUTTER")) {
            if (removeOne(p, Material.CHICKEN) && removeCustom(p, "food_BEER") && removeCustom(p, "food_BUTTER")) {
                cookWithDelay(p, rank, 280, CustomFoodType.BEER_CHICKEN, 1, true, null);
                return;
            }
        }
        p.sendMessage("§c[요리] §f프라이팬에 올릴 재료가 부족합니다.");
    }

    // === C. 가마솥 레시피 (국자 우클릭, 시간단축 적용) ===
    private void handleCauldron(Player p, int rank, Block cauldron) {
        // 소금 (염수 양동이 → 소금×16, 10s = 200t)
        if (hasCustomItem(p, "bucket_salt") && removeCustom(p, "bucket_salt")) {
            cookWithDelay(p, rank, 200, CustomFoodType.SALT, 16, true, () -> returnContainer(p, Material.BUCKET));
            return;
        }
        // 생크림 (우유+설탕, 6s = 120t)
        if (hasItem(p, Material.MILK_BUCKET) && hasItem(p, Material.SUGAR)) {
            if (removeOne(p, Material.MILK_BUCKET) && removeOne(p, Material.SUGAR)) {
                cookWithDelay(p, rank, 120, CustomFoodType.CREAM, 1, true, () -> returnContainer(p, Material.BUCKET));
                return;
            }
        }
        // 치즈 (우유+소금, 8s = 160t)
        if (hasItem(p, Material.MILK_BUCKET) && hasCustomItem(p, "food_SALT")) {
            if (removeOne(p, Material.MILK_BUCKET) && removeCustom(p, "food_SALT")) {
                cookWithDelay(p, rank, 160, CustomFoodType.CHEESE, 1, true, () -> returnContainer(p, Material.BUCKET));
                return;
            }
        }
        // 버터 (생크림, 8s = 160t)
        if (hasCustomItem(p, "food_CREAM") && removeCustom(p, "food_CREAM")) {
            cookWithDelay(p, rank, 160, CustomFoodType.BUTTER, 4, true, null);
            return;
        }
        // 두부 (식수+콩+소금, 12s = 240t)
        if (hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "crop_SOYBEAN") && hasCustomItem(p, "food_SALT")) {
            if (removeCustom(p, "bucket_drink") && removeCustom(p, "crop_SOYBEAN") && removeCustom(p, "food_SALT")) {
                cookWithDelay(p, rank, 240, CustomFoodType.TOFU, 1, true, () -> returnContainer(p, Material.BUCKET));
                return;
            }
        }
        // 밥 (식수+쌀, 8s = 160t)
        if (hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "food_POLISHED_RICE")) {
            if (removeCustom(p, "bucket_drink") && removeCustom(p, "food_POLISHED_RICE")) {
                cookWithDelay(p, rank, 160, CustomFoodType.RICE, 1, true, () -> returnContainer(p, Material.BUCKET));
                return;
            }
        }
        // 감자튀김 (대체 루트, 10s = 200t)
        if (hasCustomItem(p, "food_SLICED_POTATO") && hasCustomItem(p, "food_SALT") && hasCustomItem(p, "food_BUTTER")) {
            if (removeCustom(p, "food_SLICED_POTATO") && removeCustom(p, "food_SALT") && removeCustom(p, "food_BUTTER")) {
                cookWithDelay(p, rank, 200, CustomFoodType.FRENCH_FRIES, 1, true, null);
                return;
            }
        }

        // --- 저장형 장류 (32회) ---
        // 된장 (15s = 300t)
        if (hasCustomItem(p, "food_MEJU") && hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "food_SALT")
                && hasCustomItem(p, "crop_CHILI")) {
            if (removeCustom(p, "food_MEJU") && removeCustom(p, "bucket_drink") && removeCustom(p, "food_SALT")
                    && removeCustom(p, "crop_CHILI")) {
                startCauldronBatch(p, rank, 300, cauldron, CustomFoodType.DOENJANG, 32, true);
                return;
            }
        }
        // 고추장 (15s = 300t)
        if (hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "food_CHILI_POWDER") && hasCustomItem(p, "food_MEJU") && hasCustomItem(p, "food_SALT")) {
            if (removeCustom(p, "bucket_drink") && removeCustom(p, "food_CHILI_POWDER") && removeCustom(p, "food_CHILI_POWDER")
                    && removeCustom(p, "food_MEJU") && removeCustom(p, "food_SALT")) {
                startCauldronBatch(p, rank, 300, cauldron, CustomFoodType.GOCHUJANG, 32, true);
                return;
            }
        }

        // --- 저장형 국/찌개 (4회) ---
        // 대구탕 (15s = 300t)
        if (hasCustomItem(p, "food_COD_SASHIMI") && hasCustomItem(p, "food_SALT") && hasCustomItem(p, "food_DOENJANG")) {
            if (removeCustom(p, "bucket_drink") && removeCustom(p, "food_COD_SASHIMI") && removeCustom(p, "food_SALT")
                    && removeCustom(p, "food_DOENJANG") && removeCustom(p, "crop_GARLIC") && removeCustom(p, "food_CHILI_POWDER")
                    && removeCustom(p, "crop_RADISH") && removeCustom(p, "crop_ONION") && removeCustom(p, "crop_GREEN_ONION")
                    && removeOne(p, Material.BROWN_MUSHROOM) && removeCustom(p, "crop_CHILI") && removeCustom(p, "food_TOFU")) {
                startCauldronBatch(p, rank, 300, cauldron, CustomFoodType.COD_SOUP, 4, true);
                returnContainer(p, Material.BOWL); // 된장 그릇 반환
                return;
            }
        }
        // 버섯스튜 (12s = 240t)
        if (hasCustomItem(p, "food_BONE_BROTH") && hasItem(p, Material.BROWN_MUSHROOM) && hasItem(p, Material.RED_MUSHROOM)) {
            if (removeCustom(p, "food_BONE_BROTH") && removeOne(p, Material.BROWN_MUSHROOM) && removeOne(p, Material.BROWN_MUSHROOM)
                    && removeOne(p, Material.RED_MUSHROOM) && removeOne(p, Material.RED_MUSHROOM)
                    && removeCustom(p, "food_SALT") && removeCustom(p, "crop_GARLIC") && removeCustom(p, "crop_TURMERIC")
                    && removeCustom(p, "food_BUTTER") && removeCustom(p, "crop_ONION")) {
                startCauldronBatch(p, rank, 240, cauldron, CustomFoodType.MUSHROOM_STEW, 4, true);
                returnContainer(p, Material.BUCKET); // 사골육수 양동이 반환
                return;
            }
        }
        // 비트스프 (12s = 240t)
        if (hasCustomItem(p, "bucket_drink") && hasItem(p, Material.BEETROOT)) {
            // need 6 beetroot
            int count = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it != null && it.getType() == Material.BEETROOT && !hasGrade(it)) count += it.getAmount();
            }
            if (count >= 6 && hasCustomItem(p, "food_BUTTER") && hasCustomItem(p, "crop_ONION") && hasCustomItem(p, "food_SALT")) {
                removeCustom(p, "bucket_drink");
                for (int i = 0; i < 6; i++) removeOne(p, Material.BEETROOT);
                removeCustom(p, "food_BUTTER");
                removeCustom(p, "crop_ONION");
                removeCustom(p, "food_SALT");
                startCauldronBatch(p, rank, 240, cauldron, CustomFoodType.BEET_SOUP, 4, true);
                returnContainer(p, Material.BUCKET);
                return;
            }
        }
        // 소고기카레 (15s = 300t, 저장형 4회)
        if (hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "crop_TURMERIC") && hasItem(p, Material.CARROT)
                && hasItem(p, Material.BEEF) && hasCustomItem(p, "crop_ONION") && hasCustomItem(p, "food_BUTTER") && hasItem(p, Material.POTATO)) {
            if (removeCustom(p, "bucket_drink") && removeCustom(p, "crop_TURMERIC") && removeOne(p, Material.CARROT)
                    && removeOne(p, Material.BEEF) && removeCustom(p, "crop_ONION") && removeCustom(p, "food_BUTTER") && removeOne(p, Material.POTATO)) {
                startCauldronBatch(p, rank, 300, cauldron, CustomFoodType.BEEF_CURRY, 4, true);
                returnContainer(p, Material.BUCKET);
                return;
            }
        }
        // 치킨카레 (15s = 300t, 저장형 4회)
        if (hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "crop_TURMERIC") && hasItem(p, Material.CARROT)
                && hasItem(p, Material.CHICKEN) && hasCustomItem(p, "crop_ONION") && hasCustomItem(p, "food_BUTTER") && hasItem(p, Material.POTATO)) {
            if (removeCustom(p, "bucket_drink") && removeCustom(p, "crop_TURMERIC") && removeOne(p, Material.CARROT)
                    && removeOne(p, Material.CHICKEN) && removeCustom(p, "crop_ONION") && removeCustom(p, "food_BUTTER") && removeOne(p, Material.POTATO)) {
                startCauldronBatch(p, rank, 300, cauldron, CustomFoodType.CHICKEN_CURRY, 4, true);
                returnContainer(p, Material.BUCKET);
                return;
            }
        }
        // 사골육수 (15s = 300t)
        if (hasCustomItem(p, "bucket_drink") && hasItem(p, Material.BONE)) {
            if (removeCustom(p, "bucket_drink") && removeOne(p, Material.BONE)) {
                cookWithDelay(p, rank, 300, CustomFoodType.BONE_BROTH, 1, true, null);
                return;
            }
        }
        // 메주 (20s = 400t)
        if (hasCustomItem(p, "bucket_drink") && hasCustomItem(p, "crop_SOYBEAN")) {
            // need 32 soybean
            int count = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                String id = getItemId(it);
                if ("crop_SOYBEAN".equals(id) && !hasGrade(it)) count += it.getAmount();
            }
            if (count >= 32 && hasCustomItem(p, "food_SALT")) {
                removeCustom(p, "bucket_drink");
                removeCustom(p, "food_SALT");
                for (int i = 0; i < 32; i++) removeCustom(p, "crop_SOYBEAN");
                cookWithDelay(p, rank, 400, CustomFoodType.MEJU, 1, true, () -> returnContainer(p, Material.BUCKET));
                return;
            }
        }
        p.sendMessage("§c[요리] §f가마솥에 넣을 재료가 부족합니다.");
    }

    /** Start a cauldron batch (저장형). Sets charges on completion. */
    private void startCauldronBatch(Player p, int rank, long baseTicks, Block cauldron, CustomFoodType result, int charges, boolean reducible) {
        long ticks = baseTicks;
        if (reducible && rank < TIME_REDUCTION.length) {
            ticks = (long)(baseTicks * (1.0 - TIME_REDUCTION[rank]));
        }
        if (ticks < 1) ticks = 1;

        p.sendMessage("§e[요리] §f가마솥 조리 시작... (" + String.format("%.1f", ticks / 20.0) + "초)");

        final int snapRank = rank;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Proficiency gain
            PlayerProfile prof = store.getOrCreatePlayer(p.getUniqueId());
            int xpGain = plugin.getConfig().getInt("jobs.proficiency.xp-per-action.cook", 1);
            int oldLevel = prof.getJobProficiencyLevel(JobType.COOK);
            prof.addJobProficiency(JobType.COOK, xpGain);
            int newLevel = prof.getJobProficiencyLevel(JobType.COOK);
            if (newLevel > oldLevel) {
                p.sendMessage("§e[요리사] §f숙련도 레벨 UP! §a" + oldLevel + " → " + newLevel);
            }
            store.savePlayers();

            setCauldronState(cauldron, result.name(), charges);
            p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
            p.sendMessage("§a[요리] §f가마솥 조리 완료! §e" + result.koName() + " §f× §e" + charges + "§f회 서빙 가능.");
        }, ticks);
    }
}
