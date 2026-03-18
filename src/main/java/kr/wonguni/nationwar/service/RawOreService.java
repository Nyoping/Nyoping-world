package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.RawOreType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class RawOreService {
    private final NamespacedKey itemIdKey;
    private final NamespacedKey gradeKey;

    public RawOreService(JavaPlugin plugin) {
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
    }

    public ItemStack rawOre(RawOreType type, int grade, int amount) {
        ItemStack it = new ItemStack(Material.FLINT, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName("§f" + type.koName());
        List<String> lore = new ArrayList<>();
        lore.add("§7(원석) 제련해서 정제 광물로 변환");
        if (grade == 1) lore.add(0, "§6[고급]");
        if (grade == 2) lore.add(0, "§d[최고급]");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, "raw_" + type.name().toLowerCase());
        if (grade > 0) meta.getPersistentDataContainer().set(gradeKey, PersistentDataType.INTEGER, grade);
        it.setItemMeta(meta);
        return it;
    }
}
