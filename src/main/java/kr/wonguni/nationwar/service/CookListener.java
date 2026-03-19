package kr.wonguni.nationwar.service;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CookListener implements Listener {
    private final NamespacedKey gradeKey;

    public CookListener(JavaPlugin plugin) {
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
    }

    private boolean isFood(ItemStack it) {
        return it != null && it.getType() != Material.AIR && it.getType().isEdible();
    }

    private boolean hasGrade(ItemStack it) {
        if (it == null) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        Integer g = meta.getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
        return g != null && g > 0;
    }

    private boolean isBlockedDevice(InventoryType t) {
        return t == InventoryType.FURNACE || t == InventoryType.BLAST_FURNACE;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        InventoryType type = top.getType();
        int raw = e.getRawSlot();
        boolean topInv = raw < top.getSize();
        if (!topInv) return;
        if (raw != 0) return;

        ItemStack moving = e.getClick().isShiftClick() ? e.getCurrentItem() : e.getCursor();

        if (isBlockedDevice(type) && isFood(moving)) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player p) {
                p.sendMessage("§c[요리] §f음식은 훈연기에서만 조리할 수 있습니다.");
            }
            return;
        }

        if (hasGrade(moving)) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player p) {
                p.sendMessage("§c[요리] §f고급/최고급 재료는 제작 재료로 사용할 수 없습니다.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        InventoryType type = top.getType();
        if (!(isBlockedDevice(type) || type == InventoryType.SMOKER)) return;

        ItemStack it = e.getOldCursor();
        if (it == null) return;

        for (int raw : e.getRawSlots()) {
            if (raw >= top.getSize()) continue;
            if (raw != 0) continue;

            if (isBlockedDevice(type) && isFood(it)) {
                e.setCancelled(true);
                if (e.getWhoClicked() instanceof Player p) {
                    p.sendMessage("§c[요리] §f음식은 훈연기에서만 조리할 수 있습니다.");
                }
                return;
            }

            if (hasGrade(it)) {
                e.setCancelled(true);
                if (e.getWhoClicked() instanceof Player p) {
                    p.sendMessage("§c[요리] §f고급/최고급 재료는 제작 재료로 사용할 수 없습니다.");
                }
                return;
            }
        }
    }
}
