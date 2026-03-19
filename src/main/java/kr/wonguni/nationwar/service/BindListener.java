/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.ItemDespawnEvent
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package kr.wonguni.nationwar.service;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import kr.wonguni.nationwar.service.BindService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class BindListener
implements Listener {
    private final BindService bind;
    private final Plugin plugin;

    public BindListener(Plugin plugin, BindService bind) {
        this.plugin = plugin;
        this.bind = bind;
    }

    @EventHandler(ignoreCancelled=true)
    public void onInvClick(InventoryClickEvent e) {
        boolean abs;
        boolean isOwner;
        Optional<UUID> owner;
        boolean moveToOther;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        boolean bl = moveToOther = e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
        if (current != null && this.bind.isBound(current) && (owner = this.bind.getOwner(current)).isPresent()) {
            isOwner = owner.get().equals(p.getUniqueId());
            abs = this.bind.isAbsolute(current);
            if (!isOwner) {
                e.setCancelled(true);
                p.sendMessage("\u00a7c\uadc0\uc18d \uc544\uc774\ud15c\uc740 \uc8fc\uc778\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return;
            }
            if (abs) {
                if (moveToOther) {
                    e.setCancelled(true);
                    p.sendMessage("\u00a7c\uc808\ub300\uadc0\uc18d \uc544\uc774\ud15c\uc740 \ubcf4\uad00\ud568\uc73c\ub85c \uc62e\uae38 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                if (e.getClickedInventory() != null && e.getView().getTopInventory() == e.getClickedInventory() && e.getView().getTopInventory() != p.getInventory()) {
                    e.setCancelled(true);
                    p.sendMessage("\u00a7c\uc808\ub300\uadc0\uc18d \uc544\uc774\ud15c\uc740 \ubcf4\uad00\ud568\uc5d0 \ub123\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return;
                }
            }
        }
        if (cursor != null && this.bind.isBound(cursor) && (owner = this.bind.getOwner(cursor)).isPresent()) {
            isOwner = owner.get().equals(p.getUniqueId());
            abs = this.bind.isAbsolute(cursor);
            if (!isOwner) {
                e.setCancelled(true);
                p.sendMessage("\u00a7c\uadc0\uc18d \uc544\uc774\ud15c\uc740 \uc8fc\uc778\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return;
            }
            if (abs && e.getClickedInventory() != null && e.getView().getTopInventory() == e.getClickedInventory() && e.getView().getTopInventory() != p.getInventory()) {
                e.setCancelled(true);
                p.sendMessage("\u00a7c\uc808\ub300\uadc0\uc18d \uc544\uc774\ud15c\uc740 \ubcf4\uad00\ud568\uc5d0 \ub123\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onInvDrag(InventoryDragEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        ItemStack cursor = e.getOldCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }
        Optional<UUID> owner = this.bind.getOwner(cursor);
        if (owner.isEmpty()) {
            return;
        }
        boolean isOwner = owner.get().equals(p.getUniqueId());
        boolean abs = this.bind.isAbsolute(cursor);
        if (!isOwner) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uadc0\uc18d \uc544\uc774\ud15c\uc740 \uc8fc\uc778\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return;
        }
        if (abs && e.getView().getTopInventory() != null && e.getView().getTopInventory() != p.getInventory()) {
            int topSize = e.getView().getTopInventory().getSize();
            Iterator iterator = e.getRawSlots().iterator();
            while (iterator.hasNext()) {
                int raw = (Integer)iterator.next();
                if (raw >= topSize) continue;
                e.setCancelled(true);
                p.sendMessage("\u00a7c\uc808\ub300\uadc0\uc18d \uc544\uc774\ud15c\uc740 \ubcf4\uad00\ud568\uc5d0 \ub123\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        Item itemEntity = e.getItemDrop();
        ItemStack item = itemEntity.getItemStack();
        Optional<UUID> ownerOpt = this.bind.getOwner(item);
        if (ownerOpt.isEmpty()) {
            return;
        }
        UUID owner = ownerOpt.get();
        if (!owner.equals(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uadc0\uc18d \uc544\uc774\ud15c\uc740 \ub4dc\ub86d\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        if (this.bind.isAbsolute(item)) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uc808\ub300\uadc0\uc18d \uc544\uc774\ud15c\uc740 \ub4dc\ub86d\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Player ownerPlayer = Bukkit.getPlayer((UUID)owner);
        if (ownerPlayer == null) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uc8fc\uc778\uc774 \uc624\ud504\ub77c\uc778\uc774\ub77c \ub4dc\ub86d\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!itemEntity.isValid()) {
                return;
            }
            if (!ownerPlayer.isOnline()) {
                return;
            }
            ItemStack stack = itemEntity.getItemStack();
            itemEntity.remove();
            ownerPlayer.getInventory().addItem(new ItemStack[]{stack});
            ownerPlayer.sendMessage("\u00a7e[\uadc0\uc18d] \uc544\uc774\ud15c\uc774 \uc778\ubca4\ud1a0\ub9ac\ub85c \ud68c\uc218\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
        }, 200L);
    }

    @EventHandler(ignoreCancelled=true)
    public void onDespawn(ItemDespawnEvent e) {
        if (this.bind.isBound(e.getEntity().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onItemDamage(EntityDamageEvent e) {
        Item it;
        Entity entity = e.getEntity();
        if (entity instanceof Item && this.bind.isBound((it = (Item)entity).getItemStack())) {
            e.setCancelled(true);
        }
    }
}

