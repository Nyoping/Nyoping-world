/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.service;

import java.lang.invoke.CallSite;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.model.StatType;
import kr.wonguni.nationwar.service.JobService;
import kr.wonguni.nationwar.service.NationService;
import kr.wonguni.nationwar.service.StatService;
import kr.wonguni.nationwar.service.WarService;
import kr.wonguni.nationwar.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GuiService
implements Listener {
    private final JavaPlugin plugin;
    private final JobService jobService;
    private final StatService statService;
    private final NationService nationService;
    private final WarService warService;
    private final Map<UUID, GuiType> open = new HashMap<UUID, GuiType>();
    private final Map<UUID, UUID> leaderTransferTarget = new HashMap<UUID, UUID>();
    private final Map<UUID, Nation> leaderTransferNation = new HashMap<UUID, Nation>();
    private final Map<UUID, String> nationContext = new HashMap<UUID, String>();
    private final Map<UUID, Location> chestContext = new HashMap<UUID, Location>();

    public GuiService(JobService jobService, StatService statService, NationService nationService, WarService warService) {
        this.jobService = jobService;
        this.statService = statService;
        this.nationService = nationService;
        this.warService = warService;
        this.plugin = (JavaPlugin)Bukkit.getPluginManager().getPlugin("NationWar");
    }

    private String formatUntilNextUpkeep() {
        ZonedDateTime next = TimeUtil.nextKstMidnight();
        long millis = TimeUtil.millisUntil(next);
        long hours = millis / 3600000L;
        long minutes = millis / 60000L % 60L;
        return hours + "\uc2dc\uac04 " + minutes + "\ubd84";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void openPlayerMenu(Player p) {
        PlayerProfile prof;
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)27, (String)"\u00a70\ud50c\ub808\uc774\uc5b4 \uba54\ub274");
        inv.setItem(11, this.item(Material.BOOK, "\u00a7d\uc9c1\uc5c5 \uc120\ud0dd", List.of("\u00a77\uc9c1\uc5c5 GUI\ub97c \uc5fd\ub2c8\ub2e4.")));
        inv.setItem(13, this.item(Material.NETHERITE_SWORD, "\u00a7c\uc2a4\ud0ef \uac15\ud654", List.of("\u00a77\uc2a4\ud0ef GUI\ub97c \uc5fd\ub2c8\ub2e4.", "\u00a77(\uc2a4\ud0ef \uac15\ud654 1\ud68c = \uc804\ud22c\ub808\ubca8 +1)")));
        inv.setItem(15, this.item(Material.BEACON, "\u00a7b\uad6d\uac00 \uba54\ub274", List.of("\u00a77\uad6d\uac00\uac00 \uc788\uc73c\uba74 \uad6d\uac00 GUI\ub97c \uc5fd\ub2c8\ub2e4.", "\u00a77\ub125\uc11c\uc2a4 \uc6b0\ud074\ub9ad\uc73c\ub85c\ub3c4 \uc5f4 \uc218 \uc788\uc2b5\ub2c8\ub2e4.")));
        DataStore dataStore = this.nationService.getStore();
        synchronized (dataStore) {
            prof = this.nationService.getStore().getOrCreatePlayer(p.getUniqueId());
        }
        boolean ownOn = prof.isShowOwnBorders();
        boolean otherOn = prof.isShowOtherBorders();
        inv.setItem(20, this.item(ownOn ? Material.LIME_DYE : Material.GRAY_DYE, "\u00a7b\ub0b4 \uad6d\uac00 \uacbd\uacc4 \ud45c\uc2dc: " + (ownOn ? "\u00a7aON" : "\u00a7cOFF"), List.of("\u00a77\ud074\ub9ad\ud558\uc5ec \ucf1c\uae30/\ub044\uae30", "\u00a77(\ud30c\ud2f0\ud074\ub85c \uacbd\uacc4 \ud45c\uc2dc)")));
        inv.setItem(22, this.item(otherOn ? Material.ORANGE_DYE : Material.GRAY_DYE, "\u00a76\ub2e4\ub978 \uad6d\uac00 \uacbd\uacc4 \ud45c\uc2dc: " + (otherOn ? "\u00a7aON" : "\u00a7cOFF"), List.of("\u00a77\ud074\ub9ad\ud558\uc5ec \ucf1c\uae30/\ub044\uae30", "\u00a77(\uc8fc\ubcc0 \uad6d\uac00 \uacbd\uacc4 \ud45c\uc2dc)")));
        inv.setItem(26, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of("\u00a77\ud074\ub9ad\ud558\uc5ec \ub2eb\uae30")));
        this.open.put(p.getUniqueId(), GuiType.PLAYER_MENU);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    public void openStats(Player p) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)27, (String)"\u00a70\uc2a4\ud0ef \uac15\ud654");
        PlayerProfile prof = this.statService.getProfile(p.getUniqueId());
        int pts = prof.getStatPoints();
        int cl = prof.getCombatLevel();
        inv.setItem(4, this.glow(this.item(Material.EXPERIENCE_BOTTLE, "\u00a7e\uc2a4\ud0ef\ud3ec\uc778\ud2b8", List.of("\u00a77\ud604\uc7ac: \u00a7f" + pts, "\u00a77\uc804\ud22c\ub808\ubca8: \u00a7f" + cl, "\u00a77(\uc2a4\ud0ef\uc744 \uc62c\ub9b4 \ub54c\ub9c8\ub2e4 \uc804\ud22c\ub808\ubca8 +1)"))));
        inv.setItem(11, this.statItem(StatType.POWER, prof));
        inv.setItem(13, this.statItem(StatType.HUNGER, prof));
        inv.setItem(15, this.statItem(StatType.HEALTH, prof));
        inv.setItem(18, this.item(Material.ARROW, "\u00a7a\ub4a4\ub85c", List.of("\u00a77\ud50c\ub808\uc774\uc5b4 \uba54\ub274\ub85c \ub3cc\uc544\uac11\ub2c8\ub2e4.")));
        inv.setItem(26, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of("\u00a77\ud074\ub9ad\ud558\uc5ec \ub2eb\uae30")));
        this.open.put(p.getUniqueId(), GuiType.STATS);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private ItemStack statItem(StatType type, PlayerProfile prof) {
        Material mat = switch (type) {
            default -> throw new MatchException(null, null);
            case StatType.POWER -> Material.IRON_SWORD;
            case StatType.HUNGER -> Material.BREAD;
            case StatType.HEALTH -> Material.GOLDEN_APPLE;
        };
        int v = prof.getStat(type);
        return this.item(mat, "\u00a7f" + this.korean(type) + " \u00a77(Lv." + v + ")", List.of("\u00a77\ud074\ub9ad: \uc2a4\ud0ef\ud3ec\uc778\ud2b8 1 \uc18c\ube44", "\u00a77\ud604\uc7ac \uc2a4\ud0ef\ud3ec\uc778\ud2b8: \u00a7f" + prof.getStatPoints()));
    }

    private String korean(StatType t) {
        return switch (t) {
            default -> throw new MatchException(null, null);
            case StatType.POWER -> "\uacf5\uaca9";
            case StatType.HUNGER -> "\ubc30\uace0\ud514";
            case StatType.HEALTH -> "\uccb4\ub825";
        };
    }

    public void openNationMenu(Player p, Nation n) {
        if (n == null) {
            p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)54, (String)("\u00a70\uad6d\uac00: \u00a7b" + n.getName()));
        if (!n.hasNexus()) {
            p.sendMessage("\u00a7c\ud604\uc7ac \uad6d\uac00 \ub125\uc11c\uc2a4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4. \ub125\uc11c\uc2a4\ub97c \ub2e4\uc2dc \uc124\uce58\ud558\uae30 \uc804\uae4c\uc9c0 \uad6d\uac00 \uae30\ub2a5\uc744 \uc0ac\uc6a9\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        long costMult = this.plugin.getConfig().getLong("nation.upgrade-cost-multiplier", 5000L);
        long upkeepMult = this.plugin.getConfig().getLong("nation.daily-upkeep-multiplier", 1000L);
        long nextCost = (long)(Math.max(0, n.getLevel()) + 1) * costMult;
        inv.setItem(4, this.glow(this.item(Material.BEACON, "\u00a7b\uad6d\uac00 \uc815\ubcf4", List.of("\u00a77\ub808\ubca8: \u00a7f" + n.getLevel(), "\u00a77\uad6d\uace0: \u00a7f" + n.getTreasury(), "\u00a77\uccb4\ub0a9\uc77c\uc218: \u00a7f" + n.getArrearsDays(), "\u00a77\uccb4\ub0a9\uc561: \u00a7f" + (long)n.getArrearsDays() * (long)n.getLevel() * upkeepMult, "\u00a77\ub2e4\uc74c \uc720\uc9c0\ube44(\uc608\uc0c1): \u00a7f" + (long)n.getLevel() * upkeepMult, "\u00a77\ub2e4\uc74c \uc815\uc0b0\uae4c\uc9c0: \u00a7f" + this.formatUntilNextUpkeep()))));
        boolean isMember = n.isMember(p.getUniqueId());
        boolean canManage = isMember && n.isOfficer(p.getUniqueId());
        boolean isLeader = isMember && p.getUniqueId().equals(n.getLeaderUuid());
        ItemStack up = this.item(Material.ANVIL, "\u00a7a\uad6d\uac00 \ub808\ubca8 \uc5c5\uadf8\ub808\uc774\ub4dc", List.of("\u00a77\ub2e4\uc74c \ub808\ubca8 \ube44\uc6a9: \u00a7f" + nextCost, canManage ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \uc5c5\uadf8\ub808\uc774\ub4dc" : "\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5"));
        inv.setItem(6, canManage ? this.glow(up) : up);
        inv.setItem(7, this.glow(this.item(Material.IRON_SWORD, "\u00a76\uc804\uc7c1 \ub9e4\uce6d", List.of("\u00a77\ub2e4\ub978 \uad6d\uac00\ub97c \uc120\ud0dd\ud558\uc5ec \uc804\uc7c1 \ub9e4\uce6d\uc744 \uc2dc\uc791\ud569\ub2c8\ub2e4.", "\u00a7e\ud074\ub9ad\ud558\uc5ec \uad6d\uac00 \ubaa9\ub85d \uc5f4\uae30"))));
        inv.setItem(5, this.glow(this.item(Material.ENDER_PEARL, "\u00a7a\uad6d\uac00\ub85c \ud154\ub808\ud3ec\ud2b8", List.of("\u00a77\uad6d\uac00 \ub125\uc11c\uc2a4 \uc704\uce58\ub85c \uc774\ub3d9\ud569\ub2c8\ub2e4.", "\u00a7e\ud074\ub9ad\ud558\uc5ec \uc774\ub3d9"))));
        long tMult = this.plugin.getConfig().getLong("structures.turret.upgrade-cost-multiplier", 5000L);
        long iMult = this.plugin.getConfig().getLong("structures.inhibitor.upgrade-cost-multiplier", 5000L);
        long tCost = (long)(n.getTurretLevel() + 1) * tMult;
        long iCost = (long)(n.getInhibitorLevel() + 1) * iMult;
        ItemStack turretBtn = this.item(Material.DISPENSER, "\u00a7b\ud3ec\ud0d1 \uac15\ud654", List.of("\u00a77\ud604\uc7ac \ub808\ubca8: \u00a7f" + n.getTurretLevel(), "\u00a77\ub2e4\uc74c \uac15\ud654 \ube44\uc6a9: \u00a7f" + tCost, canManage ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \uac15\ud654" : "\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5"));
        ItemStack inhibBtn = this.item(Material.END_ROD, "\u00a7b\uc5b5\uc81c\uae30 \uac15\ud654", List.of("\u00a77\ud604\uc7ac \ub808\ubca8: \u00a7f" + n.getInhibitorLevel(), "\u00a77\ub2e4\uc74c \uac15\ud654 \ube44\uc6a9: \u00a7f" + iCost, canManage ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \uac15\ud654" : "\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5"));
        inv.setItem(0, canManage ? this.glow(turretBtn) : turretBtn);
        inv.setItem(8, canManage ? this.glow(inhibBtn) : inhibBtn);
        ItemStack dep1 = this.item(Material.EMERALD, "\u00a7a\uad6d\uace0 \uc785\uae08 \u00a77(+1,000)", List.of("\u00a77\uac1c\uc778 \ub3c8 -> \uad6d\uace0", isMember ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \uc785\uae08" : "\u00a78(\uc5f4\ub78c \uc804\uc6a9)"));
        ItemStack dep5 = this.item(Material.EMERALD, "\u00a7a\uad6d\uace0 \uc785\uae08 \u00a77(+5,000)", List.of("\u00a77\uac1c\uc778 \ub3c8 -> \uad6d\uace0", isMember ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \uc785\uae08" : "\u00a78(\uc5f4\ub78c \uc804\uc6a9)"));
        ItemStack dep10 = this.item(Material.EMERALD, "\u00a7a\uad6d\uace0 \uc785\uae08 \u00a77(+10,000)", List.of("\u00a77\uac1c\uc778 \ub3c8 -> \uad6d\uace0", isMember ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \uc785\uae08" : "\u00a78(\uc5f4\ub78c \uc804\uc6a9)"));
        inv.setItem(45, isMember ? this.glow(dep1) : dep1);
        inv.setItem(46, isMember ? this.glow(dep5) : dep5);
        inv.setItem(47, isMember ? this.glow(dep10) : dep10);
        if (p.hasPermission("nationwar.admin")) {
            inv.setItem(48, this.glow(this.item(Material.HOPPER, "\u00a7c\uad6d\uace0 \ucd9c\uae08 \u00a77(-1,000)", List.of("\u00a77\uad00\ub9ac\uc790 \uc804\uc6a9", "\u00a7e\ud074\ub9ad: \ubcf8\uc778\uc5d0\uac8c \uc9c0\uae09"))));
            inv.setItem(49, this.glow(this.item(Material.HOPPER, "\u00a7c\uad6d\uace0 \ucd9c\uae08 \u00a77(-5,000)", List.of("\u00a77\uad00\ub9ac\uc790 \uc804\uc6a9", "\u00a7e\ud074\ub9ad: \ubcf8\uc778\uc5d0\uac8c \uc9c0\uae09"))));
            inv.setItem(50, this.glow(this.item(Material.HOPPER, "\u00a7c\uad6d\uace0 \ucd9c\uae08 \u00a77(-10,000)", List.of("\u00a77\uad00\ub9ac\uc790 \uc804\uc6a9", "\u00a7e\ud074\ub9ad: \ubcf8\uc778\uc5d0\uac8c \uc9c0\uae09"))));
        }
        ItemStack inviteBtn = this.item(Material.PAPER, "\u00a7b\uad6d\uac00\uc6d0 \ucd08\ub300", List.of("\u00a77\uc628\ub77c\uc778 \ud50c\ub808\uc774\uc5b4 \ubaa9\ub85d\uc5d0\uc11c \uc120\ud0dd\ud558\uc5ec \ucd08\ub300\ud569\ub2c8\ub2e4.", canManage ? "\u00a7e\ud074\ub9ad\ud558\uc5ec \ucd08\ub300" : "\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5"));
        inv.setItem(51, canManage ? this.glow(inviteBtn) : inviteBtn);
        inv.setItem(52, this.glow(this.item(Material.WRITABLE_BOOK, "\u00a7e\uad6d\uace0 \uae30\ub85d", List.of("\u00a77\ucd5c\uadfc \uc785\uae08/\ucd9c\uae08 \uae30\ub85d\uc744 \ud655\uc778\ud569\ub2c8\ub2e4.", "\u00a7e\ud074\ub9ad: \ucc44\ud305\uc73c\ub85c \ucd9c\ub825"))));
        ArrayList<UUID> members = new ArrayList<UUID>(n.getMembers());
        members.sort(Comparator.comparing(UUID::toString));
        int slot = 9;
        for (UUID u : members) {
            Player online;
            if (slot >= 54) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)u);
            String name = op.getName() != null ? op.getName() : u.toString().substring(0, 8);
            boolean leader = u.equals(n.getLeaderUuid());
            boolean officer = n.isOfficer(u);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta)skull.getItemMeta();
            sm.setOwningPlayer(op);
            sm.setDisplayName((leader ? "\u00a76" : "\u00a7f") + name);
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("\u00a77" + (leader ? "\uad6d\uac00\uc7a5" : (officer ? "\uac04\ubd80" : "\uad6d\uac00\uc6d0")));
            if (isLeader && !leader) {
                lore.add("\u00a7e\ud074\ub9ad: \uac04\ubd80 " + (officer ? "\ud574\uc81c" : "\uc9c0\uc815"));
            }
            if ((online = Bukkit.getPlayer((UUID)u)) != null) {
                PlayerProfile prof = this.statService.getProfile(u);
                lore.add("\u00a77\uc804\ud22cLv: \u00a7f" + prof.getCombatLevel());
                lore.add("\u00a77K/D/A: \u00a7f" + prof.getKills() + "/" + prof.getDeaths() + "/" + prof.getAssists());
            }
            sm.setLore(lore);
            skull.setItemMeta((ItemMeta)sm);
            inv.setItem(slot++, skull);
        }
        boolean moveOn = n.isRelocationMode();
        long cd = 86400000L;
        long last = n.getLastRelocationAt();
        long now = System.currentTimeMillis();
        long left = last <= 0L ? 0L : Math.max(0L, cd - (now - last));
        String cdLine = left <= 0L ? "\u00a7a\ucfe8\ud0c0\uc784 \uc5c6\uc74c" : "\u00a7c\ucfe8\ud0c0\uc784 " + left / 3600000L + "\uc2dc\uac04 " + left / 60000L % 60L + "\ubd84";
        inv.setItem(24, this.item(Material.COMPASS, "\u00a7e\ub125\uc11c\uc2a4 \uc62e\uae30\uae30 \ubaa8\ub4dc: " + (moveOn ? "\u00a7aON" : "\u00a7cOFF"), List.of("\u00a77(\uad6d\uac00\uc7a5\ub9cc)", "\u00a77\ubaa8\ub4dc\ub97c ON \ud558\uba74", "\u00a77\ub125\uc11c\uc2a4 \uc124\uce58\uad8c\uc73c\ub85c \uc6b0\ud074\ub9ad \uc2dc", "\u00a77\uae30\uc874 \ub125\uc11c\uc2a4\uac00 \uc0ac\ub77c\uc9c0\uace0", "\u00a77\uc0c8 \uc704\uce58\uc5d0 \uc7ac\uc124\uce58\ub429\ub2c8\ub2e4.", "\u00a77\uc131\uacf5 \uc2dc \uc790\ub3d9 OFF", cdLine, "\u00a7e\ud074\ub9ad: ON/OFF")));
        if (p.getUniqueId().equals(n.getLeaderUuid())) {
            inv.setItem(23, this.item(Material.NAME_TAG, "\u00a7e\uad6d\uac00\uc7a5 \ub118\uae30\uae30", List.of("\u00a77(\uad6d\uac00\uc7a5\ub9cc)", "\u00a77\uad6d\uac00\uc6d0 \uc911 \ud55c \uba85\uc744", "\u00a77\uc0c8 \uad6d\uac00\uc7a5\uc73c\ub85c \uac15\uc81c \uc9c0\uc815\ud569\ub2c8\ub2e4.", "\u00a7c\ub418\ub3cc\ub9b4 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "\u00a7e\ud074\ub9ad: \ub300\uc0c1 \uc120\ud0dd")));
            inv.setItem(22, this.item(Material.TNT, "\u00a74\uad6d\uac00 \uba78\ub9dd", List.of("\u00a77(\uad6d\uac00\uc7a5\ub9cc)", "\u00a77\uad6d\uac00 \ub370\uc774\ud130\uc640 \ub125\uc11c\uc2a4\ub97c", "\u00a77\uc644\uc804\ud788 \ucd08\uae30\ud654\ud558\uace0", "\u00a77\ubb34\uc18c\uc18d\uc73c\ub85c \ub3cc\uc544\uac11\ub2c8\ub2e4.", "\u00a7c\uc870\uac74: \ucd1d \uad6d\uac00\uc6d0 2\uba85 \uc774\ud558", "\u00a74\ud074\ub9ad: \uc7ac\ud655\uc778")));
        }
        if (n.isMember(p.getUniqueId()) && !p.getUniqueId().equals(n.getLeaderUuid())) {
            inv.setItem(25, this.glow(this.item(Material.OAK_DOOR, "\u00a7e\uad6d\uac00 \ud0c8\ud1f4", List.of("\u00a77\uad6d\uac00\uc5d0\uc11c \ud0c8\ud1f4\ud569\ub2c8\ub2e4.", "\u00a7c(\uad6d\uac00\uc7a5\uc740 \ud0c8\ud1f4 \ubd88\uac00)", "\u00a7e\ud074\ub9ad\ud558\uc5ec \uc9c4\ud589"))));
        }
        inv.setItem(53, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of("\u00a77\ud074\ub9ad\ud558\uc5ec \ub2eb\uae30")));
        this.open.put(p.getUniqueId(), GuiType.NATION);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    public void openWarMatchMenu(Player p, Nation own) {
        if (own == null) {
            p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)54, (String)"\u00a70\uc804\uc7c1 \ub9e4\uce6d");
        inv.setItem(49, this.glow(this.item(Material.ARROW, "\u00a7e\ub4a4\ub85c", List.of("\u00a77\uad6d\uac00 \uba54\ub274\ub85c \ub3cc\uc544\uac11\ub2c8\ub2e4."))));
        inv.setItem(53, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of()));
        int slot = 0;
        for (Nation n : this.nationService.getAllNations()) {
            if (n == null || n.getName().equalsIgnoreCase(own.getName()) || !n.hasNexus()) continue;
            if (slot >= 45) break;
            inv.setItem(slot++, this.glow(this.item(Material.PAPER, "\u00a7a" + n.getName(), List.of("\u00a77\ud074\ub9ad: \uc804\uc7c1 \ub9e4\uce6d \uc2dc\uc791"))));
        }
        this.open.put(p.getUniqueId(), GuiType.WAR_MATCH);
        p.openInventory(inv);
    }

    public void openInviteSelectMenu(Player p, Nation nation) {
        if (nation == null) {
            p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        if (!nation.isOfficer(p.getUniqueId())) {
            p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
            return;
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)54, (String)"\u00a70\uad6d\uac00 \ucd08\ub300");
        inv.setItem(49, this.glow(this.item(Material.ARROW, "\u00a7e\ub4a4\ub85c", List.of("\u00a77\uad6d\uac00 \uba54\ub274\ub85c \ub3cc\uc544\uac11\ub2c8\ub2e4."))));
        inv.setItem(53, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of()));
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            if (online.getUniqueId().equals(p.getUniqueId()) || nation.isMember(online.getUniqueId()) || this.nationService.getNationOf(online.getUniqueId()) != null) continue;
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta)skull.getItemMeta();
            sm.setOwningPlayer((OfflinePlayer)online);
            sm.setDisplayName("\u00a7a" + online.getName());
            sm.setLore(List.of("\u00a77\ud074\ub9ad: \ucd08\ub300 \uc694\uccad \ubcf4\ub0b4\uae30"));
            skull.setItemMeta((ItemMeta)sm);
            inv.setItem(slot++, this.glow(skull));
        }
        this.nationContext.put(p.getUniqueId(), nation.getName());
        this.open.put(p.getUniqueId(), GuiType.INVITE_SELECT);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
    }

    public void openLeaveConfirmMenu(Player p, Nation nation) {
        if (nation == null) {
            p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)27, (String)"\u00a70\uad6d\uac00 \ud0c8\ud1f4 \ud655\uc778");
        inv.setItem(11, this.glow(this.item(Material.RED_WOOL, "\u00a7c\uc815\ub9d0 \ud0c8\ud1f4", List.of("\u00a77\uad6d\uac00\uc5d0\uc11c \ud0c8\ud1f4\ud569\ub2c8\ub2e4."))));
        inv.setItem(15, this.glow(this.item(Material.LIME_WOOL, "\u00a7a\ucde8\uc18c", List.of("\u00a77\ub3cc\uc544\uac00\uae30"))));
        this.nationContext.put(p.getUniqueId(), nation.getName());
        this.open.put(p.getUniqueId(), GuiType.LEAVE_CONFIRM);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
    }

    public void openLeaderTransferSelectMenu(Player p, Nation nation) {
        if (nation == null) {
            return;
        }
        if (!p.getUniqueId().equals(nation.getLeaderUuid())) {
            p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
            return;
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)54, (String)"\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30");
        inv.setItem(49, this.glow(this.item(Material.ARROW, "\u00a7e\ub4a4\ub85c", List.of("\u00a77\uad6d\uac00 \uba54\ub274\ub85c \ub3cc\uc544\uac11\ub2c8\ub2e4."))));
        inv.setItem(53, this.item(Material.BARRIER, "\u00a7c\ub2eb\uae30", List.of()));
        int slot = 0;
        for (UUID u : nation.getMembers()) {
            if (u.equals(nation.getLeaderUuid())) continue;
            if (slot >= 45) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)u);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta)skull.getItemMeta();
            sm.setOwningPlayer(op);
            String name = op.getName() == null ? u.toString().substring(0, 8) : op.getName();
            sm.setDisplayName("\u00a7a" + name);
            sm.setLore(List.of("\u00a77\ud074\ub9ad: \uc774 \uad6d\uac00\uc6d0\uc5d0\uac8c \uad6d\uac00\uc7a5 \ub118\uae30\uae30"));
            skull.setItemMeta((ItemMeta)sm);
            inv.setItem(slot++, this.glow(skull));
        }
        this.leaderTransferNation.put(p.getUniqueId(), nation);
        this.open.put(p.getUniqueId(), GuiType.LEADER_TRANSFER_SELECT);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
    }

    public void openLeaderTransferConfirmMenu(Player p, Nation nation, UUID target) {
        if (nation == null || target == null) {
            return;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)target);
        String name = op.getName() == null ? target.toString().substring(0, 8) : op.getName();
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)27, (String)"\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30 \ud655\uc778");
        inv.setItem(11, this.glow(this.item(Material.RED_WOOL, "\u00a7c\ub118\uae30\uae30", List.of("\u00a77\ub300\uc0c1: \u00a7e" + name, "\u00a7c\ub418\ub3cc\ub9b4 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4."))));
        inv.setItem(15, this.glow(this.item(Material.LIME_WOOL, "\u00a7a\ucde8\uc18c", List.of("\u00a77\ub3cc\uc544\uac00\uae30"))));
        this.leaderTransferNation.put(p.getUniqueId(), nation);
        this.leaderTransferTarget.put(p.getUniqueId(), target);
        this.open.put(p.getUniqueId(), GuiType.LEADER_TRANSFER_CONFIRM);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
    }

    public void openCollapseConfirmMenu(Player p, Nation nation) {
        if (nation == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)p, (int)27, (String)"\u00a70\uad6d\uac00 \uba78\ub9dd \ud655\uc778");
        inv.setItem(11, this.glow(this.item(Material.RED_WOOL, "\u00a74\uba78\ub9dd", List.of("\u00a77\uad6d\uac00\ub97c \uc644\uc804\ud788 \ucd08\uae30\ud654\ud569\ub2c8\ub2e4.", "\u00a7c\ub418\ub3cc\ub9b4 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4."))));
        inv.setItem(15, this.glow(this.item(Material.LIME_WOOL, "\u00a7a\ucde8\uc18c", List.of("\u00a77\ub3cc\uc544\uac00\uae30"))));
        this.nationContext.put(p.getUniqueId(), nation.getName());
        this.open.put(p.getUniqueId(), GuiType.COLLAPSE_CONFIRM);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Nation n;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        GuiType t = this.open.get(p.getUniqueId());
        if (t == null) {
            return;
        }
        if (e.getView() == null) {
            return;
        }
        String title = e.getView().getTitle();
        if (t == GuiType.PLAYER_MENU && !title.equals("\u00a70\ud50c\ub808\uc774\uc5b4 \uba54\ub274")) {
            return;
        }
        if (t == GuiType.STATS && !title.equals("\u00a70\uc2a4\ud0ef \uac15\ud654")) {
            return;
        }
        if (t == GuiType.NATION && !title.startsWith("\u00a70\uad6d\uac00: ")) {
            return;
        }
        if (t == GuiType.WAR_MATCH && !title.equals("\u00a70\uc804\uc7c1 \ub9e4\uce6d")) {
            return;
        }
        if (t == GuiType.INVITE_SELECT && !title.equals("\u00a70\uad6d\uac00 \ucd08\ub300")) {
            return;
        }
        if (t == GuiType.LEAVE_CONFIRM && !title.equals("\u00a70\uad6d\uac00 \ud0c8\ud1f4 \ud655\uc778")) {
            return;
        }
        if (t == GuiType.LEADER_TRANSFER_SELECT && !title.equals("\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30")) {
            return;
        }
        if (t == GuiType.LEADER_TRANSFER_CONFIRM && !title.equals("\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30 \ud655\uc778")) {
            return;
        }
        if (t == GuiType.COLLAPSE_CONFIRM && !title.equals("\u00a70\uad6d\uac00 \uba78\ub9dd \ud655\uc778")) {
            return;
        }
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        Material m = clicked.getType();
        if (m == Material.BARRIER) {
            p.closeInventory();
            return;
        }
        if (t == GuiType.LEADER_TRANSFER_SELECT) {
            SkullMeta sm;
            OfflinePlayer op;
            ItemMeta im;
            Nation n2 = this.leaderTransferNation.get(p.getUniqueId());
            if (m == Material.ARROW) {
                if (n2 != null) {
                    this.openNationMenu(p, n2);
                }
                return;
            }
            if (m == Material.PLAYER_HEAD && n2 != null && (im = clicked.getItemMeta()) instanceof SkullMeta && (op = (sm = (SkullMeta)im).getOwningPlayer()) != null) {
                this.openLeaderTransferConfirmMenu(p, n2, op.getUniqueId());
            }
            return;
        }
        if (t == GuiType.LEADER_TRANSFER_CONFIRM) {
            Nation n3 = this.leaderTransferNation.get(p.getUniqueId());
            UUID target = this.leaderTransferTarget.get(p.getUniqueId());
            if (m == Material.LIME_WOOL) {
                if (n3 != null) {
                    this.openLeaderTransferSelectMenu(p, n3);
                }
                return;
            }
            if (m == Material.RED_WOOL && n3 != null && target != null) {
                boolean ok = this.nationService.transferLeader(p, target);
                DataStore op = this.warService.getStore();
                synchronized (op) {
                    this.warService.getStore().saveNations();
                }
                if (ok) {
                    this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId()));
                }
            }
            return;
        }
        if (t == GuiType.COLLAPSE_CONFIRM) {
            Nation n4 = this.nationService.getNationOf(p.getUniqueId());
            if (m == Material.LIME_WOOL) {
                if (n4 != null) {
                    this.openNationMenu(p, n4);
                }
                return;
            }
            if (m == Material.RED_WOOL && n4 != null) {
                if (!p.getUniqueId().equals(n4.getLeaderUuid())) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                    return;
                }
                if (n4.getMembers().size() > 2) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc6d0\uc774 2\uba85 \uc774\ud558\uc77c \ub54c\ub9cc \uba78\ub9dd\uc2dc\ud0ac \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                this.warService.removePlacedNexus(n4);
                this.nationService.collapse(p);
                p.closeInventory();
            }
            return;
        }
        if (t == GuiType.PLAYER_MENU) {
            int slot;
            if (m == Material.BOOK) {
                this.jobService.openJobGui(p);
                return;
            }
            if (m == Material.NETHERITE_SWORD) {
                this.openStats(p);
                return;
            }
            if (m == Material.BEACON) {
                Nation n5 = this.nationService.getNationOf(p.getUniqueId());
                if (n5 == null) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4. \ub125\uc11c\uc2a4\ub97c \uc124\uce58\ud558\uba74 \uad6d\uac00\uac00 \uc124\ub9bd\ub429\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.openNationMenu(p, n5);
                return;
            }
            if (!(m != Material.LIME_DYE && m != Material.ORANGE_DYE && m != Material.GRAY_DYE || (slot = e.getRawSlot()) != 20 && slot != 22)) {
                DataStore target = this.nationService.getStore();
                synchronized (target) {
                    PlayerProfile prof = this.nationService.getStore().getOrCreatePlayer(p.getUniqueId());
                    if (slot == 20) {
                        prof.setShowOwnBorders(!prof.isShowOwnBorders());
                    }
                    if (slot == 22) {
                        prof.setShowOtherBorders(!prof.isShowOtherBorders());
                    }
                    this.nationService.getStore().savePlayers();
                }
                this.openPlayerMenu(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                return;
            }
        }
        if (t == GuiType.NATION) {
            boolean isLeader;
            String nationName = title.replace("\u00a70\uad6d\uac00: \u00a7b", "");
            n = this.nationService.getNationByName(nationName);
            if (n == null) {
                p.sendMessage("\u00a7c\uad6d\uac00 \uc815\ubcf4\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                p.closeInventory();
                return;
            }
            boolean isMember = n.isMember(p.getUniqueId());
            boolean canManage = isMember && n.isOfficer(p.getUniqueId());
            boolean bl = isLeader = isMember && p.getUniqueId().equals(n.getLeaderUuid());
            if (m == Material.WRITABLE_BOOK) {
                List<String> logs = n.getTreasuryLogs();
                p.sendMessage("\u00a7e[\uad6d\uace0 \uae30\ub85d] \u00a77\ucd5c\uadfc " + logs.size() + "\uac1c");
                if (logs.isEmpty()) {
                    p.sendMessage("\u00a77\uae30\ub85d\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                } else {
                    for (String line : logs) {
                        p.sendMessage(line);
                    }
                }
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                return;
            }
            if (!isMember && m != Material.WRITABLE_BOOK) {
                if (m == Material.BARRIER) {
                    p.closeInventory();
                    return;
                }
                p.sendMessage("\u00a77(\uc5f4\ub78c \uc804\uc6a9) \ub2e4\ub978 \uad6d\uac00\uc758 \uba54\ub274\ub294 \uae30\ub2a5\uc744 \uc0ac\uc6a9\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.4f);
                return;
            }
            if (m == Material.ENDER_PEARL) {
                if (!n.hasNexus() || n.getNexusWorld() == null) {
                    p.sendMessage("\u00a7c\uad6d\uac00 \ub125\uc11c\uc2a4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                World w = Bukkit.getWorld((String)n.getNexusWorld());
                if (w == null) {
                    p.sendMessage("\u00a7c\ub125\uc11c\uc2a4 \uc6d4\ub4dc\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4: " + n.getNexusWorld());
                    return;
                }
                Location loc = new Location(w, (double)n.getNexusX() + 0.5, (double)n.getNexusY() + 1.0, (double)n.getNexusZ() + 0.5);
                loc = w.getHighestBlockAt(loc).getLocation().add(0.5, 1.0, 0.5);
                p.closeInventory();
                p.teleport(loc);
                p.sendMessage("\u00a7a[\uad6d\uac00] \u00a7f\uad6d\uac00 \ub125\uc11c\uc2a4\ub85c \uc774\ub3d9\ud588\uc2b5\ub2c8\ub2e4.");
                return;
            }
            if (m == Material.IRON_SWORD) {
                if (!canManage) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uc804\uc7c1 \ub9e4\uce6d\uc744 \uc2dc\uc791\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.openWarMatchMenu(p, n);
                return;
            }
            if (m == Material.COMPASS) {
                long now2;
                long cd;
                long remain;
                if (!isLeader) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \ub125\uc11c\uc2a4 \uc62e\uae30\uae30\ub97c \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                if (!n.isRelocationMode() && (remain = (cd = 86400000L) - ((now2 = System.currentTimeMillis()) - n.getLastRelocationAt())) > 0L) {
                    long mins = Math.max(1L, remain / 60000L);
                    p.sendMessage("\u00a7c\ub125\uc11c\uc2a4 \uc62e\uae30\uae30\ub294 \ucfe8\ud0c0\uc784\uc785\ub2c8\ub2e4. (\uc57d " + mins + "\ubd84 \ub0a8\uc74c)");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                n.setRelocationMode(!n.isRelocationMode());
                DataStore ds = this.warService.getStore();
                synchronized (ds) {
                    this.warService.getStore().saveNations();
                }
                this.openNationMenu(p, n);
                return;
            }
            if (m == Material.NAME_TAG) {
                if (!isLeader) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.openLeaderTransferSelectMenu(p, n);
                return;
            }
            if (m == Material.TNT) {
                if (!isLeader) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.openCollapseConfirmMenu(p, n);
                return;
            }
            if (m == Material.OAK_DOOR) {
                if (!isMember) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc6d0\uc774 \uc544\ub2d9\ub2c8\ub2e4.");
                    return;
                }
                if (isLeader) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\uc740 \ud0c8\ud1f4\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                this.openLeaveConfirmMenu(p, n);
                return;
            }
            if (m == Material.PAPER) {
                if (!canManage) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \ucd08\ub300\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.openInviteSelectMenu(p, n);
                return;
            }
            if (m == Material.ANVIL) {
                if (!canManage) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.nationService.upgrade(p);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId())));
                return;
            }
            if (m == Material.DISPENSER) {
                if (!canManage) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.nationService.upgradeTurret(p);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId())));
                return;
            }
            if (m == Material.END_ROD) {
                if (!canManage) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.nationService.upgradeInhibitor(p);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId())));
                return;
            }
            if (m == Material.EMERALD) {
                long amt = 0L;
                int s = e.getRawSlot();
                switch (s) {
                    case 45: {
                        amt = 1000L;
                        break;
                    }
                    case 46: {
                        amt = 5000L;
                        break;
                    }
                    case 47: {
                        amt = 10000L;
                        break;
                    }
                    default: {
                        amt = 0L;
                    }
                }
                if (amt > 0L) {
                    boolean ok = this.nationService.depositToTreasury(p, amt);
                    if (!ok) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    } else {
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
                    }
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId())));
                }
                return;
            }
            if (m == Material.HOPPER) {
                long amt = 0L;
                if (!p.hasPermission("nationwar.admin")) {
                    p.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                int s = e.getRawSlot();
                switch (s) {
                    case 48: {
                        amt = 1000L;
                        break;
                    }
                    case 49: {
                        amt = 5000L;
                        break;
                    }
                    case 50: {
                        amt = 10000L;
                        break;
                    }
                    default: {
                        amt = 0L;
                    }
                }
                if (amt > 0L) {
                    boolean ok = this.nationService.adminWithdrawFromTreasury(n.getName(), p.getUniqueId(), amt);
                    if (!ok) {
                        p.sendMessage("\u00a7c\uad6d\uace0\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    } else {
                        p.sendMessage("\u00a7a[\uad6d\uace0] \u00a7f" + amt + " \ucd9c\uae08(\uad00\ub9ac\uc790)");
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
                    }
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId())));
                }
                return;
            }
            if (m == Material.PAPER) {
                if (!canManage) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                this.openInviteSelectMenu(p, n);
                return;
            }
            if (m == Material.OAK_DOOR) {
                if (!isMember) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc6d0\uc774 \uc544\ub2d9\ub2c8\ub2e4.");
                    return;
                }
                if (isLeader) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\uc740 \ud0c8\ud1f4\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                this.openLeaveConfirmMenu(p, n);
                return;
            }
            if (m == Material.WRITABLE_BOOK) {
                p.sendMessage("\u00a76[\uad6d\uace0 \uae30\ub85d] \u00a77\ucd5c\uadfc \uae30\ub85d");
                for (String line : n.getTreasuryLogs()) {
                    p.sendMessage(line);
                }
                return;
            }
            if (m == Material.PLAYER_HEAD && isLeader) {
                SkullMeta sm;
                OfflinePlayer op;
                ItemMeta meta = clicked.getItemMeta();
                if (meta instanceof SkullMeta && (op = (sm = (SkullMeta)meta).getOwningPlayer()) != null && op.getUniqueId() != null && !op.getUniqueId().equals(n.getLeaderUuid())) {
                    this.nationService.toggleOfficer(p, op.getUniqueId());
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openNationMenu(p, this.nationService.getNationOf(p.getUniqueId())));
                }
                return;
            }
        }
        if (t == GuiType.WAR_MATCH) {
            if (m == Material.BARRIER) {
                p.closeInventory();
                return;
            }
            if (m == Material.ARROW) {
                Nation own = this.nationService.getNationOf(p.getUniqueId());
                if (own != null) {
                    this.openNationMenu(p, own);
                } else {
                    p.closeInventory();
                }
                return;
            }
            if (m == Material.PAPER) {
                Nation own = this.nationService.getNationOf(p.getUniqueId());
                if (own == null) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    p.closeInventory();
                    return;
                }
                if (!own.isOfficer(p.getUniqueId())) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uc804\uc7c1 \ub9e4\uce6d\uc744 \uc2dc\uc791\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                String dn = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
                String targetName = dn.replace("\u00a7a", "").trim();
                Nation target = this.nationService.getNationByName(targetName);
                if (target == null) {
                    p.sendMessage("\u00a7c\ub300\uc0c1 \uad6d\uac00\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return;
                }
                boolean ok = this.warService.startChallenge(p, target);
                if (ok) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.2f, 1.6f);
                    p.closeInventory();
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }
        }
        if (t == GuiType.INVITE_SELECT) {
            if (m == Material.ARROW) {
                Nation own = this.nationService.getNationOf(p.getUniqueId());
                if (own != null) {
                    this.openNationMenu(p, own);
                } else {
                    p.closeInventory();
                }
                return;
            }
            if (m == Material.PLAYER_HEAD) {
                SkullMeta sm;
                OfflinePlayer op;
                String nationName = this.nationContext.get(p.getUniqueId());
                final Nation finalN = nationName == null ? null : this.nationService.getNationByName(nationName);
                if (finalN == null) {
                    p.sendMessage("\u00a7c\uad6d\uac00 \uc815\ubcf4\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    p.closeInventory();
                    return;
                }
                if (!finalN.isOfficer(p.getUniqueId())) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5/\uac04\ubd80\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                ItemMeta meta = clicked.getItemMeta();
                if (meta instanceof SkullMeta && (op = (sm = (SkullMeta)meta).getOwningPlayer()) != null && op.getUniqueId() != null) {
                    this.nationService.invite(p, op);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openInviteSelectMenu(p, finalN));
                }
                return;
            }
        }
        if (t == GuiType.LEAVE_CONFIRM) {
            if (m == Material.LIME_WOOL) {
                Nation own = this.nationService.getNationOf(p.getUniqueId());
                if (own != null) {
                    this.openNationMenu(p, own);
                } else {
                    p.closeInventory();
                }
                return;
            }
            if (m == Material.RED_WOOL) {
                boolean ok = this.nationService.leaveNation(p);
                p.closeInventory();
                if (ok) {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }
        }
        if (t == GuiType.STATS) {
            if (m == Material.ARROW) {
                this.openPlayerMenu(p);
                return;
            }
            StatType type = null;
            if (m == Material.IRON_SWORD) {
                type = StatType.POWER;
            }
            if (m == Material.BREAD) {
                type = StatType.HUNGER;
            }
            if (m == Material.GOLDEN_APPLE) {
                type = StatType.HEALTH;
            }
            if (type != null) {
                boolean ok = this.statService.upgrade(p, type);
                if (!ok) {
                    p.sendMessage("\u00a7c\uc2a4\ud0ef\ud3ec\uc778\ud2b8\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    p.sendMessage("\u00a7a" + this.korean(type) + " \uc2a4\ud0ef\uc774 \uac15\ud654\ub418\uc5c8\uc2b5\ub2c8\ub2e4! (\uc804\ud22c\ub808\ubca8 +1)");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
                }
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openStats(p));
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        GuiType t = this.open.get(p.getUniqueId());
        if (t == null) {
            return;
        }
        if (e.getView() == null) {
            return;
        }
        String title = e.getView().getTitle();
        if (t == GuiType.PLAYER_MENU && !title.equals("\u00a70\ud50c\ub808\uc774\uc5b4 \uba54\ub274")) {
            return;
        }
        if (t == GuiType.STATS && !title.equals("\u00a70\uc2a4\ud0ef \uac15\ud654")) {
            return;
        }
        if (t == GuiType.NATION && !title.startsWith("\u00a70\uad6d\uac00: ")) {
            return;
        }
        if (t == GuiType.WAR_MATCH && !title.equals("\u00a70\uc804\uc7c1 \ub9e4\uce6d")) {
            return;
        }
        if (t == GuiType.INVITE_SELECT && !title.equals("\u00a70\uad6d\uac00 \ucd08\ub300")) {
            return;
        }
        if (t == GuiType.LEAVE_CONFIRM && !title.equals("\u00a70\uad6d\uac00 \ud0c8\ud1f4 \ud655\uc778")) {
            return;
        }
        if (t == GuiType.LEADER_TRANSFER_SELECT && !title.equals("\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30")) {
            return;
        }
        if (t == GuiType.LEADER_TRANSFER_CONFIRM && !title.equals("\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30 \ud655\uc778")) {
            return;
        }
        if (t == GuiType.COLLAPSE_CONFIRM && !title.equals("\u00a70\uad6d\uac00 \uba78\ub9dd \ud655\uc778")) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        HumanEntity humanEntity = e.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            boolean viewingOurGui;
            String title = p.getOpenInventory() != null ? p.getOpenInventory().getTitle() : "";
            boolean bl = viewingOurGui = title.equals("\u00a70\ud50c\ub808\uc774\uc5b4 \uba54\ub274") || title.equals("\u00a70\uc2a4\ud0ef \uac15\ud654") || title.startsWith("\u00a70\uad6d\uac00: ") || title.equals("\u00a70\uc804\uc7c1 \ub9e4\uce6d") || title.equals("\u00a70\uad6d\uac00 \ucd08\ub300") || title.equals("\u00a70\uad6d\uac00 \ud0c8\ud1f4 \ud655\uc778") || title.equals("\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30") || title.equals("\u00a70\uad6d\uac00\uc7a5 \ub118\uae30\uae30 \ud655\uc778") || title.equals("\u00a70\uad6d\uac00 \uba78\ub9dd \ud655\uc778");
            if (!viewingOurGui) {
                this.open.remove(p.getUniqueId());
            }
        });
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

    private ItemStack glow(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        m.addEnchant(Enchantment.UNBREAKING, 1, true);
        m.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        it.setItemMeta(m);
        return it;
    }

    private static enum GuiType {
        PLAYER_MENU,
        STATS,
        NATION,
        WAR_MATCH,
        INVITE_SELECT,
        LEAVE_CONFIRM,
        CHEST_LOCK,
        CHEST_SHARE,
        LEADER_TRANSFER_SELECT,
        LEADER_TRANSFER_CONFIRM,
        COLLAPSE_CONFIRM;

    }
}

