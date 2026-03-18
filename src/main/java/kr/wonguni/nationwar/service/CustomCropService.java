package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.CustomCropType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CustomCropService {
    private final NamespacedKey itemIdKey;

    public CustomCropService(JavaPlugin plugin) {
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
    }

    public ItemStack seedItem(CustomCropType type, int amount) {
        ItemStack it = new ItemStack(Material.WHEAT_SEEDS, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName("§a" + type.koName() + " 씨앗");
        List<String> lore = new ArrayList<>();
        lore.add("§7(커스텀 작물) 리소스 임시: 씨앗");
        meta.setLore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, "crop_seed_" + type.name().toLowerCase());
        it.setItemMeta(meta);
        return it;
    }

    public ItemStack cropItem(CustomCropType type, int amount) {
        ItemStack it = new ItemStack(Material.WHEAT, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName("§f" + type.koName());
        List<String> lore = new ArrayList<>();
        lore.add("§7(커스텀 작물) 리소스 임시: 밀");
        meta.setLore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, "crop_" + type.name().toLowerCase());
        it.setItemMeta(meta);
        return it;
    }

    public CustomCropType seedType(ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id == null || !id.startsWith("crop_seed_")) return null;
        return CustomCropType.fromId(id.substring("crop_seed_".length()));
    }

    public CustomCropType cropType(ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id == null || !id.startsWith("crop_")) return null;
        if (id.startsWith("crop_seed_")) return null;
        return CustomCropType.fromId(id.substring("crop_".length()));
    }
}
