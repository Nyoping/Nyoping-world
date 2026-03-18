/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.util;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Items {
    private Items() {
    }

    public static NamespacedKey nexusKey(JavaPlugin plugin) {
        return new NamespacedKey((Plugin)plugin, "nexus_item");
    }

    public static ItemStack createNexusItem(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\ub125\uc11c\uc2a4 \uc124\uce58\uad8c");
        meta.setLore(List.of("\u00a77\uc6b0\ud074\ub9ad\uc73c\ub85c \ub125\uc11c\uc2a4\ub97c \uc124\uce58\ud569\ub2c8\ub2e4.", "\u00a77\uc124\uce58 \ub192\uc774: Y 50~300"));
        meta.getPersistentDataContainer().set(Items.nexusKey(plugin), PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isNexusItem(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte b = (Byte)meta.getPersistentDataContainer().get(Items.nexusKey(plugin), PersistentDataType.BYTE);
        return b != null && b == 1;
    }
}

