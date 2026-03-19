package kr.wonguni.nationwar.service;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * 어부 인챈트북 / 인챈트 장비 매입 공식 (문서 §9.8)
 *
 * 인챈트북 매입가 = Σ(계수 × 레벨 × 3,000)
 * 인챈트 장비 매입가 = 장비 기본가 + Σ(계수 × 레벨 × 1,000) → 내구도 감가
 * 내구도 감가: 최종가 = 감가전가 × (현재내구 / 최대내구)
 */
public class EnchantBuyService {
    private final JavaPlugin plugin;

    // 계수 6 인챈트
    private static final Set<String> COEFF_6 = Set.of(
            "mending", "silk_touch", "fortune", "looting", "luck_of_the_sea", "lure"
    );
    // 계수 0 (저주)
    private static final Set<String> COEFF_0 = Set.of(
            "binding_curse", "vanishing_curse"
    );

    public EnchantBuyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private int coeff(Enchantment ench) {
        String key = ench.getKey().getKey().toLowerCase(Locale.ROOT);
        // Check config overrides first
        List<String> cfg6 = plugin.getConfig().getStringList("economy.enchant.coeff_6");
        List<String> cfg0 = plugin.getConfig().getStringList("economy.enchant.coeff_0");
        if (cfg0.contains(key)) return 0;
        if (cfg6.contains(key)) return 6;
        if (COEFF_0.contains(key)) return 0;
        if (COEFF_6.contains(key)) return 6;
        return 3; // default
    }

    /** Calculate buy price for an enchanted book (no durability penalty). */
    public long bookPrice(ItemStack book) {
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) return 0;
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta esm)) return 0;

        int bookUnit = plugin.getConfig().getInt("economy.enchant.book_unit", 3000);
        long sum = 0;
        for (Map.Entry<Enchantment, Integer> e : esm.getStoredEnchants().entrySet()) {
            int c = coeff(e.getKey());
            int lvl = e.getValue() == null ? 0 : e.getValue();
            sum += (long) c * lvl * bookUnit;
        }
        return sum;
    }

    /** Calculate buy price for enchanted gear/armor/tool (with durability penalty). */
    public long gearPrice(ItemStack gear) {
        if (gear == null) return 0;
        ItemMeta meta = gear.getItemMeta();
        if (meta == null) return 0;

        Map<Enchantment, Integer> enchants = gear.getEnchantments();
        if (enchants.isEmpty()) return 0;

        int gearUnit = plugin.getConfig().getInt("economy.enchant.gear_unit", 1000);
        long basePrice = getBasePrice(gear.getType());

        long enchantValue = 0;
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            int c = coeff(e.getKey());
            int lvl = e.getValue() == null ? 0 : e.getValue();
            enchantValue += (long) c * lvl * gearUnit;
        }

        long preDurability = basePrice + enchantValue;

        // Durability penalty
        if (meta instanceof Damageable dmg) {
            int maxDur = gear.getType().getMaxDurability();
            if (maxDur > 0) {
                int currentDur = maxDur - dmg.getDamage();
                preDurability = (long)(preDurability * ((double) currentDur / maxDur));
            }
        }

        return preDurability;
    }

    private long getBasePrice(Material mat) {
        String key = mat.name().toLowerCase(Locale.ROOT);
        // Check config first
        long v = plugin.getConfig().getLong("economy.base_prices." + key, -1);
        if (v >= 0) return v;

        // Hardcoded defaults from doc
        if (mat == Material.BOW) return 120;
        if (mat == Material.FISHING_ROD) return 90;
        return 0;
    }
}
