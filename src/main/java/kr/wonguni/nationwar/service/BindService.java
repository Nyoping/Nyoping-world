/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BindService {
    private final NamespacedKey ownerKey;
    private final NamespacedKey absKey;
    private final NamespacedKey uidKey;

    public BindService(JavaPlugin plugin) {
        this.ownerKey = new NamespacedKey((Plugin)plugin, "bound_owner");
        this.absKey = new NamespacedKey((Plugin)plugin, "bound_abs");
        this.uidKey = new NamespacedKey((Plugin)plugin, "bound_uid");
    }

    public boolean bind(ItemStack item, UUID owner, boolean absolute) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.ownerKey, PersistentDataType.STRING, owner.toString());
        pdc.set(this.absKey, PersistentDataType.BYTE, (byte)(absolute ? 1 : 0));
        if (!pdc.has(this.uidKey, PersistentDataType.STRING)) {
            pdc.set(this.uidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        }
        ArrayList<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList();
        lore.removeIf(l -> l != null && (l.contains("[\uadc0\uc18d]") || l.contains("[\uc808\ub300\uadc0\uc18d]")));
        lore.add(0, absolute ? "\u00a7c[\uc808\ub300\uadc0\uc18d]" : "\u00a76[\uadc0\uc18d]");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return true;
    }

    public Optional<UUID> getOwner(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String s = (String)meta.getPersistentDataContainer().get(this.ownerKey, PersistentDataType.STRING);
        if (s == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(s));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isAbsolute(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte b = (Byte)meta.getPersistentDataContainer().get(this.absKey, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public boolean isBound(ItemStack item) {
        return this.getOwner(item).isPresent();
    }

    public boolean canUse(Player player, ItemStack item) {
        Optional<UUID> owner = this.getOwner(item);
        return owner.isEmpty() || owner.get().equals(player.getUniqueId());
    }
}

