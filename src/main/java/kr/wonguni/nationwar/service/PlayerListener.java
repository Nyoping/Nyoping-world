/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Slime
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerExpChangeEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package kr.wonguni.nationwar.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.model.StatType;
import kr.wonguni.nationwar.service.BindService;
import kr.wonguni.nationwar.service.BorderVisualService;
import kr.wonguni.nationwar.service.GuiService;
import kr.wonguni.nationwar.service.JobService;
import kr.wonguni.nationwar.service.MoneyService;
import kr.wonguni.nationwar.service.NationService;
import kr.wonguni.nationwar.service.ProtectionService;
import kr.wonguni.nationwar.service.StatService;
import kr.wonguni.nationwar.service.WarService;
import kr.wonguni.nationwar.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerListener
implements Listener {
    private final JavaPlugin plugin;
    private final MoneyService money;
    private final NationService nation;
    private final BindService bind;
    private final JobService jobs;
    private final ProtectionService protection;
    private final WarService war;
    private final BorderVisualService borderVisual;
    private final GuiService gui;
    private final Map<UUID, Long> lastSwapMillis = new HashMap<UUID, Long>();
    private final Map<UUID, Integer> swapCount = new HashMap<UUID, Integer>();
    private final Map<UUID, Map<UUID, Long>> assistHits = new HashMap<UUID, Map<UUID, Long>>();

    private boolean handsEmpty(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        boolean mainEmpty = main == null || main.getType().isAir();
        boolean offEmpty = off == null || off.getType().isAir();
        return mainEmpty && offEmpty;
    }

    private boolean comboRequireEmptyHands() {
        return this.plugin.getConfig().getBoolean("ui.menu-combo.require-empty-hands", false);
    }

    private boolean isComboArmed(Player p) {
        long windowMs = this.plugin.getConfig().getLong("ui.menu-combo.window-ms", 700L);
        long now = System.currentTimeMillis();
        long last = this.lastSwapMillis.getOrDefault(p.getUniqueId(), 0L);
        int cnt = this.swapCount.getOrDefault(p.getUniqueId(), 0);
        return cnt >= 2 && now - last <= windowMs;
    }

    private void consumeComboAndOpenMenu(Player p) {
        this.swapCount.remove(p.getUniqueId());
        this.lastSwapMillis.remove(p.getUniqueId());
        this.gui.openPlayerMenu(p);
    }

    public PlayerListener(JavaPlugin plugin, MoneyService money, NationService nation, BindService bind, JobService jobs, ProtectionService protection, WarService war, BorderVisualService borderVisual, GuiService gui) {
        this.plugin = plugin;
        this.money = money;
        this.nation = nation;
        this.bind = bind;
        this.jobs = jobs;
        this.protection = protection;
        this.war = war;
        this.borderVisual = borderVisual;
        this.gui = gui;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        DataStore dataStore = this.war.getStore();
        synchronized (dataStore) {
            this.war.getStore().getOrCreatePlayer(p.getUniqueId());
            this.war.getStore().savePlayers();
        }
        this.syncStatDisplay(p);
        if (!p.hasPlayedBefore() && this.plugin.getConfig().getBoolean("nexus.give-on-first-join", true)) {
            ItemStack item = Items.createNexusItem(this.plugin);
            this.bind.bind(item, p.getUniqueId(), true);
            p.getInventory().addItem(new ItemStack[]{item});
            p.sendMessage("\u00a7e[\ub125\uc11c\uc2a4] \u00a7f\uccab \uc811\uc18d \ubcf4\uc0c1\uc73c\ub85c \u00a7c\uc808\ub300\uadc0\uc18d\u00a7f \ub125\uc11c\uc2a4 \uc124\uce58\uad8c\uc744 \uc9c0\uae09\ud588\uc2b5\ub2c8\ub2e4.");
        }
        this.sendResourcePacks(p);
        Nation n = this.nation.getNationOf(p.getUniqueId());
        if (n != null && n.hasNexus()) {
            this.war.checkAndRepairNexus(n);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onQuit(PlayerQuitEvent e) {
        this.assistHits.remove(e.getPlayer().getUniqueId());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item;
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && Items.isNexusItem(this.plugin, item = e.getItem())) {
            Player p = e.getPlayer();
            Nation n = this.nation.getNationOf(p.getUniqueId());
            if (p.isSneaking() && n != null) {
                this.gui.openNationMenu(p, n);
                e.setCancelled(true);
                return;
            }
            if (n == null) {
                String defaultName = p.getName();
                DataStore dataStore = this.war.getStore();
                synchronized (dataStore) {
                    Object name = defaultName;
                    int idx = 1;
                    while (this.war.getStore().getNationByName((String)name) != null) {
                        name = defaultName + ++idx;
                        if (idx <= 9999) continue;
                    }
                    n = new Nation((String)name, p.getUniqueId());
                    this.war.getStore().putNation(n);
                    this.war.getStore().setNationOf(p.getUniqueId(), n);
                    this.war.getStore().saveNations();
                    this.war.getStore().savePlayers();
                }
                p.sendMessage("\u00a7a[\uad6d\uac00] \u00a7f\ub125\uc11c\uc2a4 \uc124\uce58\ub85c \uad6d\uac00\uac00 \uc124\ub9bd\ub429\ub2c8\ub2e4: \u00a7e" + n.getName());
            }
            if (!(!n.hasNexus() || p.getUniqueId().equals(n.getLeaderUuid()) && n.isRelocationMode())) {
                p.sendMessage("\u00a7c\uc774\ubbf8 \uad6d\uac00 \ub125\uc11c\uc2a4\uac00 \uc874\uc7ac\ud569\ub2c8\ub2e4. (\uc6c5\ud06c\ub9ac\uae30+\uc6b0\ud074\ub9ad: \uad6d\uac00 \uba54\ub274)");
                return;
            }
            if (e.getClickedBlock() == null) {
                p.sendMessage("\u00a7c\ube14\ub85d\uc744 \ubc14\ub77c\ubcf4\uace0 \uc6b0\ud074\ub9ad\ud558\uc5ec \uc124\uce58\ud558\uc138\uc694.");
                return;
            }
            Location place = e.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
            int y = place.getBlockY();
            int minY = this.plugin.getConfig().getInt("nexus.place.min-y", 50);
            int maxY = this.plugin.getConfig().getInt("nexus.place.max-y", 300);
            if (y < minY || y > maxY) {
                p.sendMessage("\u00a7c\ub125\uc11c\uc2a4\ub294 Y " + minY + "~" + maxY + "\uc5d0\uc11c\ub9cc \uc124\uce58 \uac00\ub2a5\ud569\ub2c8\ub2e4.");
                return;
            }
            if (n.hasNexus() && p.getUniqueId().equals(n.getLeaderUuid()) && n.isRelocationMode()) {
                long cd = 86400000L;
                long last = n.getLastRelocationAt();
                long now = System.currentTimeMillis();
                if (last > 0L && now - last < cd) {
                    long left = cd - (now - last);
                    long hours = left / 3600000L;
                    long minutes = left / 60000L % 60L;
                    p.sendMessage("\u00a7c\ub125\uc11c\uc2a4 \uc62e\uae30\uae30\ub294 24\uc2dc\uac04 \ucfe8\ud0c0\uc784\uc774 \uc788\uc2b5\ub2c8\ub2e4. \ub0a8\uc740 \uc2dc\uac04: " + hours + "\uc2dc\uac04 " + minutes + "\ubd84");
                    return;
                }
            }
            int cxCheck = place.getBlockX();
            int czCheck = place.getBlockZ();
            if (this.nation.territoryOverlapsOtherNations(n, place.getWorld().getName(), cxCheck, czCheck, n.getLevel())) {
                p.sendMessage("\u00a7c\uc774 \uc704\uce58\uc758 \ubcf4\ud638\uad6c\uc5ed\uc774 \ub2e4\ub978 \uad6d\uac00 \ubcf4\ud638\uad6c\uc5ed\uacfc \uacb9\uce69\ub2c8\ub2e4. \ub2e4\ub978 \uc704\uce58\uc5d0 \uc124\uce58\ud558\uc138\uc694.");
                return;
            }
            long cost = (long)Math.max(0, n.getLevel()) * 1000L;
            if (cost > 0L && !this.money.withdraw(p.getUniqueId(), cost)) {
                p.sendMessage("\u00a7c\uad6d\uac00 \uc124\ub9bd/\uc7ac\uc124\uce58 \ube44\uc6a9\uc774 \ubd80\uc871\ud569\ub2c8\ub2e4. \ud544\uc694: " + cost);
                return;
            }
            if (cost > 0L) {
                p.sendMessage("\u00a7e[\uad6d\uac00] \u00a7f\uc124\ub9bd/\uc7ac\uc124\uce58 \ube44\uc6a9 \u00a7a" + cost + "\u00a7f \uc774(\uac00) \uc9c0\ubd88\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
            }
            int cx = place.getBlockX();
            int cy = place.getBlockY();
            int cz = place.getBlockZ();
            DataStore dataStore = this.war.getStore();
            synchronized (dataStore) {
                if (n.hasNexus() && p.getUniqueId().equals(n.getLeaderUuid()) && n.isRelocationMode()) {
                    this.war.removePlacedNexus(n);
                    n.setLastRelocationAt(System.currentTimeMillis());
                    n.setRelocationMode(false);
                }
                n.setNexus(place.getWorld().getName(), cx, cy, cz);
                this.war.getStore().saveNations();
            }
            this.war.spawnPlacedNexus(place, n);
            p.sendMessage("\u00a7a\ub125\uc11c\uc2a4 \uc124\uce58 \uc644\ub8cc! (\ubcf4\ud638\uad6c\uc5ed \ud65c\uc131\ud654)");
            int secs = this.plugin.getConfig().getInt("ui.border-visual.default-seconds", 15);
            this.borderVisual.showNationBorder(p, n, secs);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onDamage(EntityDamageEvent e) {
        Player p;
        Slime slime;
        Entity entity = e.getEntity();
        if (entity instanceof Slime && (slime = (Slime)entity).getScoreboardTags().contains("NW_STRUCTURE")) {
            if (e instanceof EntityDamageByEntityEvent) {
                return;
            }
            e.setCancelled(true);
            return;
        }
        entity = e.getEntity();
        if (entity instanceof Player && this.protection.isInSpawnProtection((p = (Player)entity).getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Slime slime;
        Object dn;
        LivingEntity le;
        Entity pl2;
        Projectile proj;
        Entity entity;
        Player p;
        Entity entity2 = e.getEntity();
        if (entity2 instanceof Player && this.protection.isInSpawnProtection((p = (Player)entity2).getLocation())) {
            e.setCancelled(true);
            return;
        }
        this.war.cancelDamageIfOverworldNexus(e.getEntity(), (EntityDamageEvent)e);
        Player damager = null;
        Entity entity3 = e.getDamager();
        if (entity3 instanceof Player) {
            Player pl;
            damager = pl = (Player)entity3;
        }
        if (e.getDamager() instanceof Projectile) {
            proj = (Projectile) e.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }
        // if (damager != null && (pl2 = e.getEntity()) instanceof LivingEntity && !((le = (LivingEntity)pl2) instanceof Player)) {
        //     this.spawnDamageIndicator(le.getLocation(), e.getFinalDamage());
        // }
        if (damager != null && (pl2 = e.getEntity()) instanceof Player) {
            Player victim = (Player)pl2;
            dn = this.nation.getNationOf(damager.getUniqueId());
            Nation vn = this.nation.getNationOf(victim.getUniqueId());
            if (dn != null && vn != null && ((Nation)dn).getName().equalsIgnoreCase(vn.getName())) {
                return;
            }
            this.assistHits.computeIfAbsent(victim.getUniqueId(), k -> new HashMap()).put(damager.getUniqueId(), System.currentTimeMillis());
        }
        if ((dn = e.getEntity()) instanceof Slime && (slime = (Slime)dn).getScoreboardTags().contains("NW_STRUCTURE")) {
            Player src = damager;
            double amount = e.getFinalDamage();
            e.setCancelled(true);
            this.war.damageStructure(slime, amount, src);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        boolean canPay;
        PlayerProfile kprof;
        PlayerProfile vprof;
        Player victim = e.getEntity();
        e.setDroppedExp(0);
        e.setKeepLevel(true);
        DataStore dataStore = this.war.getStore();
        synchronized (dataStore) {
            PlayerProfile vp = this.war.getStore().getOrCreatePlayer(victim.getUniqueId());
            int currentFood = victim.getFoodLevel();
            vp.setLastFoodLevel(currentFood);
            this.war.getStore().savePlayers();
        }
        if (this.protection.isInSpawnProtection(victim.getLocation())) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            return;
        }
        Nation kn = this.nation.getNationOf(killer.getUniqueId());
        Nation vn = this.nation.getNationOf(victim.getUniqueId());
        if (kn != null && vn != null && kn.getName().equalsIgnoreCase(vn.getName())) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            e.setKeepLevel(true);
            return;
        }
        long percent = this.plugin.getConfig().getLong("money.pvp-steal-percent", 10L);
        long perCombat = this.plugin.getConfig().getLong("money.pvp-steal-per-combat-level", 100L);
        DataStore dataStore2 = this.war.getStore();
        synchronized (dataStore2) {
            vprof = this.war.getStore().getOrCreatePlayer(victim.getUniqueId());
            kprof = this.war.getStore().getOrCreatePlayer(killer.getUniqueId());
        }
        int combatLevel = vprof.getCombatLevel();
        long victimMoney = this.money.getBalance(victim.getUniqueId());
        long steal = victimMoney * percent / 100L;
        boolean bl = canPay = victimMoney >= (steal += (long)combatLevel * perCombat);
        if (canPay) {
            this.money.withdraw(victim.getUniqueId(), steal);
            this.money.deposit(killer.getUniqueId(), steal);
            e.setKeepInventory(true);
            e.getDrops().clear();
        } else {
            if (victimMoney > 0L) {
                this.money.withdraw(victim.getUniqueId(), victimMoney);
                this.money.deposit(killer.getUniqueId(), victimMoney);
            }
            e.setKeepInventory(false);
        }
        int assistWindowSec = this.plugin.getConfig().getInt("war.match.assist-window-seconds", 15);
        long now = System.currentTimeMillis();
        HashSet<UUID> assists = new HashSet<UUID>();
        Map<UUID, Long> map = this.assistHits.getOrDefault(victim.getUniqueId(), Collections.emptyMap());
        for (Map.Entry<UUID, Long> en : map.entrySet()) {
            Nation an;
            if (en.getKey().equals(killer.getUniqueId()) || now - en.getValue() > (long)assistWindowSec * 1000L || (an = this.nation.getNationOf(en.getKey())) != null && vn != null && an.getName().equalsIgnoreCase(vn.getName())) continue;
            assists.add(en.getKey());
        }
        DataStore dataStore3 = this.war.getStore();
        synchronized (dataStore3) {
            vprof.setDeaths(vprof.getDeaths() + 1);
            kprof.setKills(kprof.getKills() + 1);
            for (UUID au : assists) {
                PlayerProfile ap = this.war.getStore().getOrCreatePlayer(au);
                ap.setAssists(ap.getAssists() + 1);
            }
            this.war.getStore().savePlayers();
        }
        this.syncStatDisplay(killer);
        this.assistHits.remove(victim.getUniqueId());
        killer.sendMessage("\u00a7e[\uc804\ud22c] \u00a7f\uc57d\ud0c8 \uc131\uacf5: \u00a7a" + steal + "\u00a7f (\uc0c1\ub300 \uc804\ud22cLv " + combatLevel + ")");
        victim.sendMessage("\u00a7c[\uc804\ud22c] \u00a7f\uc0ac\ub9dd\uc73c\ub85c \ub3c8\uc744 \uc783\uc5c8\uc2b5\ub2c8\ub2e4: \u00a7c" + Math.min(victimMoney, steal));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler(ignoreCancelled=true)
    public void onExpChange(PlayerExpChangeEvent e) {
        int amount = e.getAmount();
        if (amount <= 0) {
            return;
        }
        Player p = e.getPlayer();
        DataStore dataStore = this.war.getStore();
        synchronized (dataStore) {
            PlayerProfile prof = this.war.getStore().getOrCreatePlayer(p.getUniqueId());
            int rem = prof.getStatXpRemainder() + amount;
            int per = (int)this.plugin.getConfig().getLong("stat.xp-per-point", 100L);
            if (per <= 0) {
                per = 100;
            }
            int gained = rem / per;
            rem %= per;
            if (gained > 0) {
                prof.setStatPoints(prof.getStatPoints() + gained);
            }
            prof.setStatXpRemainder(rem);
            this.war.getStore().savePlayers();
        }
        e.setAmount(0);
        this.syncStatDisplay(p);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void syncStatDisplay(Player p) {
        PlayerProfile prof;
        DataStore dataStore = this.war.getStore();
        synchronized (dataStore) {
            prof = this.war.getStore().getOrCreatePlayer(p.getUniqueId());
        }
        int points = prof.getStatPoints();
        int per = (int)this.plugin.getConfig().getLong("stat.xp-per-point", 100L);
        if (per <= 0) {
            per = 100;
        }
        float progress = (float)prof.getStatXpRemainder() / (float)per;
        p.setTotalExperience(0);
        p.setLevel(points);
        p.setExp(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    private void sendResourcePacks(Player p) {
        if (!this.plugin.getConfig().getBoolean("resource-packs.enabled", false)) {
            return;
        }
        boolean force = this.plugin.getConfig().getBoolean("resource-packs.force", true);
        String prompt = this.plugin.getConfig().getString("resource-packs.prompt", "");
        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("resource-packs.packs");
        if (section == null) {
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            for (String key : section.getKeys(false)) {
                String url = section.getString(key + ".url", "");
                String hashStr = section.getString(key + ".hash", "");
                if (url.isEmpty()) continue;
                UUID packId = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
                byte[] hash = null;
                if (!hashStr.isEmpty()) {
                    hash = this.hexToBytes(hashStr);
                }
                try {
                    p.addResourcePack(packId, url, hash, prompt.isEmpty() ? null : prompt, force);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("[\ub9ac\uc18c\uc2a4\ud329] '" + key + "' \uc804\uc1a1 \uc2e4\ud328: " + ex.getMessage());
                }
            }
        }, 20L);
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onXpCommand(PlayerCommandPreprocessEvent e) {
        String raw;
        String msg = e.getMessage();
        if (msg == null) {
            return;
        }
        String string = raw = msg.startsWith("/") ? msg.substring(1).trim() : msg.trim();
        if (raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split("\\s+");
        String root = parts[0].toLowerCase(Locale.ROOT);
        if (!root.equals("xp") && !root.equals("experience")) {
            return;
        }
        if (parts.length < 2) {
            return;
        }
        if (this.handleXpCommand((CommandSender)e.getPlayer(), e.getPlayer(), parts)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onXpCommandFromConsole(ServerCommandEvent e) {
        String cmd = e.getCommand();
        if (cmd == null) {
            return;
        }
        String raw = cmd.trim();
        if (raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split("\\s+");
        String root = parts[0].toLowerCase(Locale.ROOT);
        if (!root.equals("xp") && !root.equals("experience")) {
            return;
        }
        if (parts.length < 2) {
            return;
        }
        boolean handled = this.handleXpCommand(e.getSender(), null, parts);
        if (handled) {
            e.setCancelled(true);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean handleXpCommand(CommandSender sender, Player selfIfPlayer, String[] parts) {
        int amount;
        if (parts.length < 4) {
            return false;
        }
        String mode = parts[1].toLowerCase(Locale.ROOT);
        if (!mode.equals("add") && !mode.equals("set")) {
            return false;
        }
        String targetToken = parts[2];
        Player target = targetToken.equalsIgnoreCase("@s") ? selfIfPlayer : Bukkit.getPlayerExact((String)targetToken);
        if (target == null) {
            sender.sendMessage("\u00a7c[\uc2a4\ud0ef\ud3ec\uc778\ud2b8] \ub300\uc0c1 \ud50c\ub808\uc774\uc5b4\ub294 \uc628\ub77c\uc778\uc774\uc5b4\uc57c \ud569\ub2c8\ub2e4. (\uc9c0\uc6d0: \ud50c\ub808\uc774\uc5b4\uc774\ub984, @s)");
            return true;
        }
        try {
            amount = Integer.parseInt(parts[3]);
        }
        catch (NumberFormatException ex) {
            sender.sendMessage("\u00a7c[\uc2a4\ud0ef\ud3ec\uc778\ud2b8] \uc218\uce58\uac00 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4: " + parts[3]);
            return true;
        }
        if (amount < 0) {
            amount = 0;
        }
        String unit = (parts.length >= 5 ? parts[4] : "points").toLowerCase(Locale.ROOT);
        boolean asLevels = unit.startsWith("l");
        int per = (int)this.plugin.getConfig().getLong("stat.xp-per-point", 100L);
        if (per <= 0) {
            per = 100;
        }
        DataStore dataStore = this.war.getStore();
        synchronized (dataStore) {
            PlayerProfile prof = this.war.getStore().getOrCreatePlayer(target.getUniqueId());
            if (mode.equals("add")) {
                if (asLevels) {
                    prof.setStatPoints(prof.getStatPoints() + amount);
                } else {
                    int rem = prof.getStatXpRemainder() + amount;
                    int gained = rem / per;
                    rem %= per;
                    if (gained > 0) {
                        prof.setStatPoints(prof.getStatPoints() + gained);
                    }
                    prof.setStatXpRemainder(rem);
                }
            } else if (asLevels) {
                prof.setStatPoints(amount);
                prof.setStatXpRemainder(0);
            } else {
                int points = amount / per;
                int rem = amount % per;
                prof.setStatPoints(points);
                prof.setStatXpRemainder(rem);
            }
            this.war.getStore().savePlayers();
        }
        this.syncStatDisplay(target);
        sender.sendMessage("\u00a7a[\uc2a4\ud0ef\ud3ec\uc778\ud2b8] \uc801\uc6a9 \uc644\ub8cc: " + target.getName() + " (" + mode + " " + amount + " " + unit + ")");
        return true;
    }

    private void spawnDamageIndicator(Location baseLoc, double damage) {
        // 몹 시각화 기능 제거
    }

    @EventHandler(ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        if ("\u00a70\uc9c1\uc5c5 \uc120\ud0dd".equals(e.getView().getTitle())) {
            e.setCancelled(true);
            this.jobs.handleClick(p, e.getRawSlot());
            return;
        }
        String title = e.getView().getTitle();
        if (title.equals("\u00a70\ud50c\ub808\uc774\uc5b4 \uba54\ub274") || title.equals("\u00a70\uc2a4\ud0ef \uac15\ud654") || title.startsWith("\u00a70\uad6d\uac00: ")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = e.getView().getTitle();
        if ("\u00a70\uc9c1\uc5c5 \uc120\ud0dd".equals(title) || title.equals("\u00a70\ud50c\ub808\uc774\uc5b4 \uba54\ub274") || title.equals("\u00a70\uc2a4\ud0ef \uac15\ud654") || title.startsWith("\u00a70\uad6d\uac00: ")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!this.plugin.getConfig().getBoolean("ui.menu-combo.enabled", true)) {
            return;
        }
        long now = System.currentTimeMillis();
        long windowMs = this.plugin.getConfig().getLong("ui.menu-combo.window-ms", 700L);
        long last = this.lastSwapMillis.getOrDefault(p.getUniqueId(), 0L);
        int cnt = this.swapCount.getOrDefault(p.getUniqueId(), 0);
        if (now - last > windowMs) {
            cnt = 0;
        }
        this.lastSwapMillis.put(p.getUniqueId(), now);
        this.swapCount.put(p.getUniqueId(), ++cnt);
    }

    @EventHandler(ignoreCancelled=true)
    public void onToggleSneakForMenu(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!this.plugin.getConfig().getBoolean("ui.menu-combo.enabled", true)) {
            return;
        }
        if (!e.isSneaking()) {
            return;
        }
        if (!this.isComboArmed(p)) {
            return;
        }
        if (this.comboRequireEmptyHands() && !this.handsEmpty(p)) {
            return;
        }
        this.consumeComboAndOpenMenu(p);
    }

    @EventHandler(ignoreCancelled=true)
    public void onRightClickStructure(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (!(entity instanceof Slime)) {
            return;
        }
        Slime slime = (Slime)entity;
        if (!slime.getScoreboardTags().contains("NW_STRUCTURE")) {
            return;
        }
        if (!slime.getScoreboardTags().contains("NW_TYPE_NEXUS")) {
            return;
        }
        String nationName = null;
        for (String tag : slime.getScoreboardTags()) {
            if (!tag.startsWith("NW_NATION_")) continue;
            nationName = tag.substring("NW_NATION_".length());
            break;
        }
        if (nationName == null || nationName.isBlank()) {
            return;
        }
        Nation n = this.nation.getNationByName(nationName);
        if (n == null) {
            return;
        }
        e.setCancelled(true);
        this.gui.openNationMenu(e.getPlayer(), n);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            DataStore dataStore = this.war.getStore();
            synchronized (dataStore) {
                PlayerProfile prof = this.war.getStore().getOrCreatePlayer(p.getUniqueId());
                p.setFoodLevel(prof.getLastFoodLevel());
                p.setSaturation(5.0f);
            }
            this.syncStatDisplay(p);
            StatService statService = new StatService(this.plugin, this.war.getStore());
            statService.apply(p);
        }, 1L);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler(ignoreCancelled=true)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        HumanEntity humanEntity = e.getEntity();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        int oldFood = p.getFoodLevel();
        int newFood = e.getFoodLevel();
        DataStore dataStore = this.war.getStore();
        synchronized (dataStore) {
            PlayerProfile prof = this.war.getStore().getOrCreatePlayer(p.getUniqueId());
            int hungerStat = prof.getStat(StatType.HUNGER);
            if (hungerStat <= 0) {
                return;
            }
            int maxReserve = Math.max(0, hungerStat);
            int reserve = Math.min(Math.max(0, prof.getHungerReserve()), maxReserve);
            if (newFood > oldFood) {
                if (newFood > 20) {
                    int overflow = newFood - 20;
                    reserve = Math.min(maxReserve, reserve + overflow);
                    e.setFoodLevel(20);
                }
                prof.setHungerReserve(reserve);
                this.war.getStore().savePlayers();
                int totalFood = Math.min(e.getFoodLevel(), 20) + reserve;
                int maxFood = 20 + hungerStat;
                if (totalFood < maxFood) {
                    this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                        if (!p.isOnline()) {
                            return;
                        }
                        if (p.getFoodLevel() >= 20) {
                            p.setFoodLevel(19);
                        }
                    }, 1L);
                }
            } else if (newFood < oldFood) {
                int loss = oldFood - newFood;
                if (reserve > 0) {
                    int absorb = Math.min(loss, reserve);
                    int remainingLoss = loss - absorb;
                    prof.setHungerReserve(reserve -= absorb);
                    this.war.getStore().savePlayers();
                    if (remainingLoss > 0) {
                        e.setFoodLevel(oldFood - remainingLoss);
                    } else {
                        e.setFoodLevel(oldFood);
                    }
                }
            }
        }
    }
}

