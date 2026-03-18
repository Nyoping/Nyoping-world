package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.CustomFoodType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CustomFoodService {
    private final NamespacedKey itemIdKey;

    public CustomFoodService(JavaPlugin plugin) {
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
    }

    public ItemStack foodItem(CustomFoodType type, int amount) {
        ItemStack it = new ItemStack(Material.BREAD, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName("§f" + type.koName());
        List<String> lore = new ArrayList<>();
        lore.add("§7(커스텀 음식) 리소스 임시: 빵");
        meta.setLore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, "food_" + type.name().toLowerCase());
        it.setItemMeta(meta);
        return it;
    }

    public CustomFoodType foodType(ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id == null || !id.startsWith("food_")) return null;
        return CustomFoodType.fromId(id.substring("food_".length()));
    }
}
