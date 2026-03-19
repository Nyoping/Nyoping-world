package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.CustomCropType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CustomCropListener implements Listener {
    private final DataStore store;
    private final CustomCropService crops;

    public CustomCropListener(DataStore store, CustomCropService crops) {
        this.store = store;
        this.crops = crops;
    }

    private String locKey(Block b) {
        return b.getWorld().getUID().toString() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlant(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null) return;
        if (clicked.getType() != Material.FARMLAND) return;

        ItemStack hand = e.getItem();
        CustomCropType type = crops.seedType(hand);
        if (type == null) return;

        Block plant = clicked.getRelative(BlockFace.UP);
        if (plant.getType() != Material.AIR) return;

        // place wheat crop as placeholder
        plant.setType(Material.WHEAT, false);
        if (plant.getBlockData() instanceof Ageable a) {
            a.setAge(0);
            plant.setBlockData(a, false);
        }

        Player p = e.getPlayer();
        store.setCropOwner(locKey(plant), p.getUniqueId());
        store.setCropType(locKey(plant), type.name());

        // consume 1 seed
        if (hand.getAmount() <= 1) {
            p.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
        p.updateInventory();

        e.setCancelled(true);
    }
}
