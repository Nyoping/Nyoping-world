package kr.wonguni.nationwar.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {
    private final ShopService shop;

    public ShopListener(ShopService shop) {
        this.shop = shop;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        boolean topInv = raw >= 0 && raw < top.getSize();

        if (top.getHolder() instanceof ShopService.CatalogHolder holder) {
            if (!topInv) return;
            e.setCancelled(true);

            if (raw == 45 && holder.getPage() > 0) {
                this.shop.openCatalogGui(p, holder.getPage() - 1);
                return;
            }
            if (raw == 49) {
                this.shop.openSellGui(p);
                return;
            }
            if (raw == 53) {
                this.shop.openCatalogGui(p, holder.getPage() + 1);
            }
            return;
        }

        if (!(top.getHolder() instanceof ShopService.SellHolder)) return;

        if (topInv) {
            if (raw == 11) {
                e.setCancelled(true);
                this.shop.openCatalogGui(p);
                return;
            }
            if (raw == 22) {
                e.setCancelled(true);
                this.shop.sell(p, top);
                return;
            }
            if (raw == 13 || raw == 15) {
                if (raw == 15) e.setCancelled(true);
                schedulePreview(top);
                return;
            }
            e.setCancelled(true);
            return;
        }

        if (e.isShiftClick()) {
            e.setCancelled(true);
            ItemStack cur = e.getCurrentItem();
            if (cur == null || cur.getType() == Material.AIR) return;
            ItemStack sell = top.getItem(13);
            if (sell == null || sell.getType() == Material.AIR || sell.getType().name().endsWith("_PANE")) {
                ItemStack moved = cur.clone();
                top.setItem(13, moved);
                e.getWhoClicked().getInventory().setItem(e.getSlot(), null);
                schedulePreview(top);
            } else {
                this.shop.fail(p, "판매칸이 비어있지 않습니다.");
            }
            return;
        }

        schedulePreview(top);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopService.SellHolder)) return;
        if (e.getRawSlots().contains(13)) {
            schedulePreview(top);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory top = e.getInventory();
        if (!(top.getHolder() instanceof ShopService.SellHolder)) return;
        ItemStack item = top.getItem(13);
        if (item != null && item.getType() != Material.AIR && !item.getType().name().endsWith("_PANE")) {
            e.getPlayer().getInventory().addItem(item);
            top.setItem(13, null);
        }
    }

    private void schedulePreview(Inventory top) {
        Bukkit.getScheduler().runTask(this.shop.getPlugin(), () -> this.shop.refreshSellPreview(top));
    }
}
