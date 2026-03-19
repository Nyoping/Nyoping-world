/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Container
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event$Result
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.service.ChestLockService;
import kr.wonguni.nationwar.service.NationService;
import kr.wonguni.nationwar.service.ProtectionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestLockListener
implements Listener {
    private final JavaPlugin plugin;
    private final NationService nationService;
    private final ProtectionService protectionService;
    private final ChestLockService chestLockService;
    private final Map<UUID, Location> editing = new HashMap<UUID, Location>();
    private final Map<UUID, String> editingNation = new HashMap<UUID, String>();
    private final Set<UUID> inGui = new HashSet<UUID>();
    private static final String TITLE_MAIN = "\u00a70\uc0c1\uc790 \uc7a0\uae08";
    private static final String TITLE_MEMBERS = "\u00a70\uacf5\uc720 \ub300\uc0c1 \uc120\ud0dd";

    public ChestLockListener(JavaPlugin plugin, NationService nationService, ProtectionService protectionService, ChestLockService chestLockService) {
        this.plugin = plugin;
        this.nationService = nationService;
        this.protectionService = protectionService;
        this.chestLockService = chestLockService;
    }

    private boolean isContainer(BlockState state) {
        return state instanceof Container;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onShiftRightClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        if (!e.getAction().isRightClick()) {
            return;
        }
        Player p = e.getPlayer();
        if (!p.isSneaking()) {
            return;
        }
        Block b = e.getClickedBlock();
        BlockState st = b.getState();
        if (!this.isContainer(st)) {
            return;
        }
        Nation own = this.nationService.getNationOf(p.getUniqueId());
        if (own == null) {
            return;
        }
        if (!own.hasNexus()) {
            p.sendMessage("\u00a7c\ub125\uc11c\uc2a4\uac00 \ucca0\uac70\ub41c \uc0c1\ud0dc\uc5d0\uc11c\ub294 \uc0ac\uc6a9\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        if (!own.isMember(p.getUniqueId())) {
            return;
        }
        Nation owner = this.protectionService.getTerritoryOwner(b.getLocation());
        if (owner == null || !owner.getName().equalsIgnoreCase(own.getName())) {
            return;
        }
        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        this.openMain(p, b.getLocation(), own.getName());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        HumanEntity humanEntity = e.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        if (e.getInventory().getLocation() == null) {
            return;
        }
        Location loc = e.getInventory().getLocation();
        ChestLockService.LockData d = this.chestLockService.get(loc);
        if (d == null) {
            return;
        }
        if (!d.locked) {
            return;
        }
        if (!this.chestLockService.canOpen(p.getUniqueId(), loc) && !p.hasPermission("nationwar.admin")) {
            e.setCancelled(true);
            p.sendMessage("\u00a7c\uc7a0\uae34 \uc0c1\uc790\uc785\ub2c8\ub2e4.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void openMain(Player p, Location loc, String nationName) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)27, (String)TITLE_MAIN);
        ChestLockService.LockData d = this.chestLockService.get(loc);
        boolean locked = d != null && d.locked;
        inv.setItem(11, this.item(locked ? Material.REDSTONE_BLOCK : Material.LIME_DYE, locked ? "\u00a7c\uc7a0\uae08 ON" : "\u00a7a\uc7a0\uae08 OFF", List.of("\u00a77\ud074\ub9ad: \uc7a0\uae08 \uc0c1\ud0dc \ud1a0\uae00")));
        inv.setItem(13, this.item(Material.PLAYER_HEAD, "\u00a7b\uacf5\uc720 \ub300\uc0c1 \uc124\uc815", List.of("\u00a77\ud074\ub9ad: \uac19\uc774 \uc5f4 \uc218 \uc788\ub294 \uad6d\uac00\uc6d0 \uc120\ud0dd", "\u00a77(\uc694\uccad\uc774 \uc544\ub2c8\ub77c \uc989\uc2dc \ucd94\uac00/\uc81c\uac70)")));
        inv.setItem(15, this.item(Material.BARRIER, "\u00a7c\uc7a0\uae08 \ub370\uc774\ud130 \uc0ad\uc81c", List.of("\u00a77\ud074\ub9ad: \uc7a0\uae08 \uc124\uc815\uc744 \uc644\uc804\ud788 \uc0ad\uc81c")));
        inv.setItem(26, this.item(Material.ARROW, "\u00a7e\ub2eb\uae30", List.of("\u00a77\ub2eb\uae30")));
        this.editing.put(p.getUniqueId(), loc);
        this.editingNation.put(p.getUniqueId(), nationName);
        this.inGui.add(p.getUniqueId());
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openMembers(Player p, Location loc, String nationName) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)54, (String)TITLE_MEMBERS);
        inv.setItem(49, this.item(Material.ARROW, "\u00a7e\ub4a4\ub85c", List.of("\u00a77\uc7a0\uae08 \uc124\uc815\uc73c\ub85c")));
        inv.setItem(53, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of()));
        ChestLockService.LockData d = this.chestLockService.getOrCreate(loc, nationName, p.getUniqueId());
        int slot = 0;
        for (OfflinePlayer op : this.chestLockService.listNationMembers(nationName)) {
            if (slot >= 45) break;
            if (op.getUniqueId() == null) continue;
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta)skull.getItemMeta();
            sm.setOwningPlayer(op);
            String name = op.getName() == null ? op.getUniqueId().toString().substring(0, 8) : op.getName();
            boolean allowed = d.allowed.contains(op.getUniqueId());
            sm.setDisplayName((allowed ? "\u00a7a" : "\u00a77") + name);
            sm.setLore(List.of(allowed ? "\u00a7a\ud604\uc7ac: \uacf5\uc720\ub428" : "\u00a7c\ud604\uc7ac: \uacf5\uc720 \uc548\ub428", "\u00a77\ud074\ub9ad: \ud1a0\uae00"));
            skull.setItemMeta((ItemMeta)sm);
            inv.setItem(slot++, skull);
        }
        this.editing.put(p.getUniqueId(), loc);
        this.editingNation.put(p.getUniqueId(), nationName);
        this.inGui.add(p.getUniqueId());
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        HumanEntity humanEntity = e.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        String title = e.getView().getTitle();
        if (!title.equals(TITLE_MAIN) && !title.equals(TITLE_MEMBERS)) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            boolean stillOurGui;
            String now = p.getOpenInventory() != null ? p.getOpenInventory().getTitle() : "";
            boolean bl = stillOurGui = now.equals(TITLE_MAIN) || now.equals(TITLE_MEMBERS);
            if (!stillOurGui) {
                this.inGui.remove(p.getUniqueId());
                this.editing.remove(p.getUniqueId());
                this.editingNation.remove(p.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        String title = e.getView().getTitle();
        if (!title.equals(TITLE_MAIN) && !title.equals(TITLE_MEMBERS)) {
            return;
        }
        e.setCancelled(true);
        if (e.getClickedInventory() == null) {
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        Location loc = this.editing.get(p.getUniqueId());
        String nationName = this.editingNation.get(p.getUniqueId());
        if (loc == null || nationName == null) {
            p.closeInventory();
            return;
        }
        Material m = clicked.getType();
        if (title.equals(TITLE_MAIN)) {
            if (m == Material.ARROW) {
                p.closeInventory();
                return;
            }
            if (m == Material.REDSTONE_BLOCK || m == Material.LIME_DYE) {
                ChestLockService.LockData d = this.chestLockService.getOrCreate(loc, nationName, p.getUniqueId());
                d.locked = !d.locked;
                this.chestLockService.save();
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openMain(p, loc, nationName));
                return;
            }
            if (m == Material.PLAYER_HEAD) {
                this.openMembers(p, loc, nationName);
                return;
            }
            if (m == Material.BARRIER) {
                this.chestLockService.remove(loc);
                p.sendMessage("\u00a7e\uc7a0\uae08 \ub370\uc774\ud130\uac00 \uc0ad\uc81c\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openMain(p, loc, nationName));
                return;
            }
        }
        if (title.equals(TITLE_MEMBERS)) {
            if (m == Material.ARROW) {
                this.openMain(p, loc, nationName);
                return;
            }
            if (m == Material.BARRIER) {
                p.closeInventory();
                return;
            }
            if (m == Material.PLAYER_HEAD) {
                SkullMeta sm = (SkullMeta)clicked.getItemMeta();
                OfflinePlayer op = sm.getOwningPlayer();
                if (op == null || op.getUniqueId() == null) {
                    return;
                }
                ChestLockService.LockData d = this.chestLockService.getOrCreate(loc, nationName, p.getUniqueId());
                UUID u = op.getUniqueId();
                if (d.allowed.contains(u)) {
                    d.allowed.remove(u);
                } else {
                    d.allowed.add(u);
                }
                this.chestLockService.save();
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openMembers(p, loc, nationName));
            }
        }
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        it.setItemMeta(meta);
        return it;
    }
}

