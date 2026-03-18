package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.CustomFoodType;
import kr.wonguni.nationwar.model.JobType;
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

import java.util.EnumSet;
import java.util.Set;

public class CookTriggerListener implements Listener {
    private final JavaPlugin plugin;
    private final JobService jobs;
    private final CustomFoodService foods;

    private final NamespacedKey itemIdKey;
    private final NamespacedKey cookRecipeKey;
    private final NamespacedKey cookChargesKey;

    private static final Set<Material> RAW_MEATS = EnumSet.of(
            Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.MUTTON, Material.RABBIT,
            Material.COD, Material.SALMON
    );

    public CookTriggerListener(JavaPlugin plugin, JobService jobs, CustomFoodService foods) {
        this.plugin = plugin;
        this.jobs = jobs;
        this.foods = foods;
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
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

    private boolean removeOne(Player p, Material mat) {
        if (p == null) return false;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != mat) continue;
            int a = it.getAmount();
            if (a <= 1) p.getInventory().setItem(i, null);
            else it.setAmount(a - 1);
            return true;
        }
        return false;
    }

    private boolean removeOneAny(Player p, Set<Material> set) {
        for (Material m : set) {
            if (removeOne(p, m)) return true;
        }
        return false;
    }

    private PersistentDataContainer pdc(Block tile) {
        if (tile == null) return null;
        if (!(tile.getState() instanceof TileState ts)) return null;
        return ts.getPersistentDataContainer();
    }

    private int getCharges(Block cauldron) {
        PersistentDataContainer pdc = pdc(cauldron);
        if (pdc == null) return 0;
        Integer v = pdc.get(cookChargesKey, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    private String getRecipe(Block cauldron) {
        PersistentDataContainer pdc = pdc(cauldron);
        if (pdc == null) return null;
        return pdc.get(cookRecipeKey, PersistentDataType.STRING);
    }

    private void setCauldronState(Block cauldron, String recipeId, int charges) {
        if (!(cauldron.getState() instanceof TileState ts)) return;
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

    private CustomFoodType recipeToFood(String recipeId) {
        if (recipeId == null) return null;
        return switch (recipeId) {
            case "stew" -> CustomFoodType.STEW;
            case "soup" -> CustomFoodType.SOUP;
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        ItemStack hand = p.getInventory().getItemInMainHand();

        // --- 가마솥 서빙(그릇 우클릭) ---
        if ((b.getType() == Material.CAULDRON || b.getType() == Material.WATER_CAULDRON) && hand.getType() == Material.BOWL) {
            int charges = getCharges(b);
            String recipe = getRecipe(b);
            if (charges <= 0 || recipe == null) return;

            CustomFoodType food = recipeToFood(recipe);
            if (food == null) return;

            ItemStack out = foods.foodItem(food, 1);
            if (!p.getInventory().addItem(out).isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), out);

            charges -= 1;
            setCauldronState(b, recipe, charges);

            p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.8f, 1.3f);
            p.sendMessage("§a[요리] §f서빙 완료. 남은 서빙: §e" + Math.max(0, charges));
            return;
        }

        // 이 아래부터는 요리사 직업만
        if (!hasCook(p)) return;

        // --- 가마솥 조리 시작(국자 우클릭) ---
        if (b.getType() == Material.CAULDRON || b.getType() == Material.WATER_CAULDRON) {
            if (!isLadle(hand)) return;

            if (!isLitCampfireBelow(b)) {
                e.setCancelled(true);
                p.sendMessage("§c[요리] §f가마솥은 불 켜진 모닥불 위에서만 조리가 가능합니다.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                return;
            }
            if (b.getType() != Material.WATER_CAULDRON) {
                e.setCancelled(true);
                p.sendMessage("§c[요리] §f가마솥에 물을 먼저 채워주세요.");
                return;
            }
            if (getCharges(b) > 0) {
                p.sendMessage("§e[요리] §f이미 서빙이 남아있습니다. 그릇으로 서빙하세요.");
                return;
            }

            boolean ok = removeOneAny(p, RAW_MEATS) || removeOne(p, Material.BROWN_MUSHROOM) || removeOne(p, Material.RED_MUSHROOM);
            if (!ok) {
                p.sendMessage("§c[요리] §f재료가 필요합니다. (임시: 생고기/버섯 1개)");
                return;
            }

            setCauldronState(b, "stew", 3);
            p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
            p.sendMessage("§a[요리] §f가마솥 조리 완료! 그릇으로 §e3§f회 서빙할 수 있습니다.");
            return;
        }

        // --- 도마(임시): BARREL + 식칼 우클릭 ---
        if (b.getType() == Material.BARREL) {
            if (!isKnife(hand)) return;

            CustomFoodType type;
            if (removeOne(p, Material.CARROT) || removeOne(p, Material.BEETROOT)) type = CustomFoodType.SALAD;
            else if (removeOne(p, Material.BREAD)) type = CustomFoodType.SANDWICH;
            else {
                p.sendMessage("§c[요리] §f재료가 필요합니다. (임시: 당근/비트/빵 1개)");
                return;
            }

            ItemStack out = foods.foodItem(type, 1);
            if (!p.getInventory().addItem(out).isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), out);
            p.sendMessage("§a[요리] §f도마 손질 완료: §f" + type.koName());
            p.playSound(p.getLocation(), Sound.ITEM_AXE_STRIP, 0.8f, 1.3f);
            return;
        }

        // --- 프라이팬(임시): STONECUTTER + 모닥불 위 + 뒤집개 우클릭 ---
        if (b.getType() == Material.STONECUTTER) {
            if (!isSpatula(hand)) return;

            if (!isLitCampfireBelow(b)) {
                e.setCancelled(true);
                p.sendMessage("§c[요리] §f프라이팬은 불 켜진 모닥불 위에서만 조리가 가능합니다.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                return;
            }

            boolean ok = removeOneAny(p, RAW_MEATS);
            if (!ok) {
                p.sendMessage("§c[요리] §f재료가 필요합니다. (임시: 생고기/생선 1개)");
                return;
            }

            ItemStack out = foods.foodItem(CustomFoodType.SKEWER, 1);
            if (!p.getInventory().addItem(out).isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), out);
            p.sendMessage("§a[요리] §f프라이팬 조리 완료: §f꼬치");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.5f);
        }
    }
}
