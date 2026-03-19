/*
 * NationWar - Sell Shop / Price List
 *  - Price list GUI from shop.yml
 *  - Sell preview GUI
 *  - Prices from shop.yml
 *  - Grade multiplier by PDC key "nw_grade" (1=고급, 2=최고급)
 *  - Custom items by PDC key "nw_item_id"
 */
package kr.wonguni.nationwar.service;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.wonguni.nationwar.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

public class ShopService implements Listener {
    private final JavaPlugin plugin;
    private final MoneyService money;
    private final JobService jobService;

    private final NamespacedKey gradeKey;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey hunterHeadKey;
    private final NamespacedKey hunterHeadTierKey;

    private File shopFile;
    private YamlConfiguration yml;

    private final Map<String, ShopEntry> byKey = new LinkedHashMap<>();
    private final Map<Material, ShopEntry> byMaterial = new EnumMap<>(Material.class);
    private final Map<String, ShopEntry> byItemId = new HashMap<>();
    private final Set<Material> denyMaterials = EnumSet.noneOf(Material.class);
    private final List<ShopEntry> catalogEntries = new ArrayList<>();

    private double multRare = 1.5;
    private double multEpic = 2.0;

    private Sound sndSuccess = Sound.ENTITY_PLAYER_LEVELUP;
    private Sound sndFail = Sound.BLOCK_NOTE_BLOCK_BASS;

    private static final int SELL_GUI_SIZE = 27;
    private static final int SELL_SLOT = 13;
    private static final int SELL_BUTTON = 22;

    private static final int CATALOG_GUI_SIZE = 54;
    private static final int DEFAULT_PAGE_ITEM_SLOTS = 36;

    public ShopService(JavaPlugin plugin, MoneyService money, JobService jobService) {
        this.plugin = plugin;
        this.money = money;
        this.jobService = jobService;
        this.gradeKey = new NamespacedKey(plugin, "nw_grade");
        this.itemIdKey = new NamespacedKey(plugin, "nw_item_id");
        this.hunterHeadKey = new NamespacedKey(plugin, "nw_hunter_head");
        this.hunterHeadTierKey = new NamespacedKey(plugin, "nw_head_tier");
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!this.shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        this.reload();
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public boolean reload() {
        try {
            this.yml = YamlConfiguration.loadConfiguration(this.shopFile);
            this.byKey.clear();
            this.byMaterial.clear();
            this.byItemId.clear();
            this.denyMaterials.clear();
            this.catalogEntries.clear();

            this.multRare = this.yml.getDouble("shop.grade_multiplier.rare", 1.5);
            this.multEpic = this.yml.getDouble("shop.grade_multiplier.epic", 2.0);

            String sSucc = this.yml.getString("shop.sounds.success", "ENTITY_PLAYER_LEVELUP");
            String sFail = this.yml.getString("shop.sounds.fail", "BLOCK_NOTE_BLOCK_BASS");
            try { this.sndSuccess = Sound.valueOf(sSucc); } catch (Exception ignored) {}
            try { this.sndFail = Sound.valueOf(sFail); } catch (Exception ignored) {}

            List<String> deny = this.yml.getStringList("shop.deny_sell_materials");
            for (String d : deny) {
                try { this.denyMaterials.add(Material.valueOf(d)); } catch (Exception ignored) {}
            }

            List<Map<?, ?>> entries = this.yml.getMapList("shop.entries");
            for (Map<?, ?> raw : entries) {
                ShopEntry e = ShopEntry.from(raw);
                if (e == null) continue;
                this.byKey.put(e.key, e);
                this.catalogEntries.add(e);
                if ("material".equals(e.type) && e.material != null) {
                    this.byMaterial.put(e.material, e);
                } else if ("custom".equals(e.type) && e.itemId != null) {
                    this.byItemId.put(e.itemId, e);
                }
            }
            return true;
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Failed to load shop.yml: " + ex.getMessage());
            return false;
        }
    }

    public void openCatalogGui(Player p) {
        openCatalogGui(p, 0);
    }

    public void openCatalogGui(Player p, int page) {
        int pageSize = getCatalogPageSize();
        int maxPage = Math.max(0, (this.catalogEntries.size() - 1) / pageSize);
        int clampedPage = Math.max(0, Math.min(page, maxPage));

        String title = this.yml != null ? this.yml.getString("shop.title", "§0상점 가격표") : "§0상점 가격표";
        Inventory inv = Bukkit.createInventory(new CatalogHolder(clampedPage), CATALOG_GUI_SIZE, title);
        fillCatalog(inv);

        int from = clampedPage * pageSize;
        int to = Math.min(this.catalogEntries.size(), from + pageSize);
        int slot = 0;
        for (int i = from; i < to; i++) {
            inv.setItem(slot++, buildCatalogItem(this.catalogEntries.get(i)));
        }

        if (clampedPage > 0) inv.setItem(45, createButton(Material.ARROW, "§e이전 페이지", "§7클릭하여 이전 페이지로 이동"));
        inv.setItem(49, createButton(Material.CHEST, "§a판매창 열기", "§7클릭하여 판매 GUI 열기"));
        if (clampedPage < maxPage) inv.setItem(53, createButton(Material.ARROW, "§e다음 페이지", "§7클릭하여 다음 페이지로 이동"));
        inv.setItem(48, createButton(Material.PAPER, "§f페이지 정보", "§7" + (clampedPage + 1) + " / " + (maxPage + 1)));
        p.openInventory(inv);
    }

    public void openSellGui(Player p) {
        String title = this.yml != null ? this.yml.getString("shop.sell_title", "§0매입 상점") : "§0매입 상점";
        Inventory inv = Bukkit.createInventory(new SellHolder(), SELL_GUI_SIZE, title);
        fillSellFrame(inv);

        inv.setItem(11, createButton(Material.BOOK, "§e가격표 보기", "§7클릭하여 가격표 GUI 열기"));

        ItemStack btn = new ItemStack(Material.LIME_DYE);
        ItemMeta bm = btn.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§a판매하기");
            bm.setLore(Arrays.asList("§7가운데 칸에 아이템을 넣고", "§7이 버튼을 눌러 판매합니다."));
            btn.setItemMeta(bm);
        }
        inv.setItem(SELL_BUTTON, btn);

        inv.setItem(15, createPreviewPlaceholder());
        inv.setItem(SELL_SLOT, createSellSlotPlaceholder());

        p.openInventory(inv);
    }

    public void refreshSellPreview(Inventory inv) {
        if (inv == null || !(inv.getHolder() instanceof SellHolder)) return;
        ItemStack in = inv.getItem(SELL_SLOT);
        if (isPlaceholder(in) || in == null || in.getType() == Material.AIR) {
            inv.setItem(SELL_SLOT, createSellSlotPlaceholder());
            inv.setItem(15, createPreviewPlaceholder());
            return;
        }

        SellResult r = evaluate(in);
        applyPriceLore(in, r);
        inv.setItem(15, createPreviewInfoItem(r, in.getAmount()));
    }

    private void fillCatalog(Inventory inv) {
        ItemStack pane = createFiller(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < getCatalogPageSize()) continue;
            inv.setItem(i, pane);
        }
    }

    private void fillSellFrame(Inventory inv) {
        ItemStack pane = createFiller(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == SELL_SLOT || i == SELL_BUTTON || i == 11 || i == 15) continue;
            inv.setItem(i, pane);
        }
    }

    public SellResult evaluate(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return SellResult.notSellable("빈 아이템입니다.");
        if (this.denyMaterials.contains(item.getType())) return SellResult.notSellable("이 아이템은 매입하지 않습니다.");

        ItemMeta meta = item.getItemMeta();
        if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
            long computed = computeEnchantedBookPrice(item, meta);
            if (computed > 0L) {
                double mult = getGradeMultiplier(item);
                return SellResult.sellable("인챈트북", Math.round(computed * mult), mult);
            }
        }
        if (meta != null && !meta.getEnchants().isEmpty()) {
            long computed = computeEnchantedGearPrice(item, meta);
            if (computed > 0L) {
                double mult = getGradeMultiplier(item);
                return SellResult.sellable(prettyMaterial(item.getType()), Math.round(computed * mult), mult);
            }
        }

        ShopEntry e = findEntry(item);
        if (e == null) return SellResult.notSellable("이 아이템은 매입하지 않습니다.");

        int amount = item.getAmount();
        double base = e.priceEach * amount;
        double mult = getGradeMultiplier(item);
        long total = Math.round(base * mult);

        String name = resolveDisplayName(e, item.getType());
        if (!e.sellable) {
            return SellResult.pricedButNotSellable(name, total, mult, "이 아이템은 가격만 존재하고 상점이 매입하지 않습니다.");
        }
        return SellResult.sellable(name, total, mult);
    }

    private ShopEntry findEntry(ItemStack item) {
        if (item.getType() == Material.POTION && isWaterBottle(item)) {
            return this.byKey.get("WATER_BOTTLE");
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            Integer hunterHead = pdc.get(this.hunterHeadKey, PersistentDataType.INTEGER);
            Integer headTier = pdc.get(this.hunterHeadTierKey, PersistentDataType.INTEGER);
            if (hunterHead != null && hunterHead == 1 && headTier != null) {
                ShopEntry headEntry = this.byKey.get("HUNTER_HEAD_T" + headTier);
                if (headEntry != null) {
                    return headEntry;
                }
            }

            String id = pdc.get(this.itemIdKey, PersistentDataType.STRING);
            if (id != null && this.byItemId.containsKey(id)) {
                return this.byItemId.get(id);
            }
        }

        return this.byMaterial.get(item.getType());
    }

    private boolean isWaterBottle(ItemStack item) {
        ItemMeta im = item.getItemMeta();
        if (!(im instanceof PotionMeta pm)) return false;
        if (pm.hasCustomEffects()) return false;
        try {
            return pm.getBasePotionType() == PotionType.WATER;
        } catch (Throwable t) {
            return false;
        }
    }

    private double getGradeMultiplier(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 1.0;
        Integer g = meta.getPersistentDataContainer().get(this.gradeKey, PersistentDataType.INTEGER);
        if (g == null) return 1.0;
        if (g == 1) return this.multRare;
        if (g == 2) return this.multEpic;
        return 1.0;
    }

    public void applyPriceLore(ItemStack item, SellResult r) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        lore.removeIf(s -> s != null && (s.contains("판매가") || s.contains("기준가") || s.contains("매입가") || s.contains("매입 불가") || s.contains("등급 배율")));
        lore.add(" ");
        if (r.ok) {
            lore.add("§e판매가: §a" + formatMoney(r.total) + getCurrencySuffix());
            if (Math.abs(r.mult - 1.0) > 0.001) {
                lore.add("§7등급 배율: §fx" + formatMultiplier(r.mult));
            }
        } else {
            if (r.listedPrice >= 0L) {
                lore.add("§6기준가: §e" + formatMoney(r.listedPrice) + getCurrencySuffix());
                if (Math.abs(r.mult - 1.0) > 0.001) {
                    lore.add("§7등급 배율: §fx" + formatMultiplier(r.mult));
                }
            }
            lore.add("§c매입 불가: §f" + r.reason);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public boolean sell(Player p, Inventory inv) {
        ItemStack in = inv.getItem(SELL_SLOT);
        if (isPlaceholder(in) || in == null || in.getType() == Material.AIR) {
            fail(p, "판매할 아이템을 넣어주세요.");
            return false;
        }

        SellResult r = evaluate(in);
        if (!r.ok) {
            fail(p, r.reason);
            refreshSellPreview(inv);
            return false;
        }

        inv.setItem(SELL_SLOT, createSellSlotPlaceholder());
        inv.setItem(15, createPreviewPlaceholder());
        this.money.deposit(p.getUniqueId(), r.total);
        p.playSound(p.getLocation(), this.sndSuccess, 1.0f, 1.0f);
        p.sendMessage("§a[상점] §f" + r.name + " §7x" + in.getAmount() + " §f판매 완료! §a+" + formatMoney(r.total) + getCurrencySuffix());
        return true;
    }

    public void fail(Player p, String msg) {
        p.playSound(p.getLocation(), this.sndFail, 1.0f, 1.0f);
        p.sendMessage("§c[상점] §f" + msg);
    }

    private String resolveDisplayName(ShopEntry e, Material fallbackMaterial) {
        if (e != null && e.displayName != null && !e.displayName.isBlank()) return e.displayName;
        if (fallbackMaterial != null && fallbackMaterial != Material.AIR) return prettyMaterial(fallbackMaterial);
        if (e != null && e.key != null) return prettyKey(e.key);
        return "알 수 없는 아이템";
    }

    private ItemStack buildCatalogItem(ShopEntry e) {
        Material icon = e.resolveIconMaterial();
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (icon == Material.POTION && meta instanceof PotionMeta pm && "WATER_BOTTLE".equalsIgnoreCase(e.key)) {
            try {
                pm.setBasePotionType(PotionType.WATER);
            } catch (Throwable ignored) {
            }
            meta = pm;
        }

        String displayName = resolveDisplayName(e, "material".equals(e.type) ? e.material : icon);
        if (e.displayName != null && !e.displayName.isBlank()) {
            meta.setDisplayName("§f" + e.displayName);
        } else if (!"material".equals(e.type) || e.material == null) {
            meta.setDisplayName("§f" + displayName);
        }

        List<String> lore = new ArrayList<>();
        lore.add((e.sellable ? "§e판매가: §a" : "§6기준가: §e") + formatMoney(e.priceEach) + getCurrencySuffix() + " §7/ 1개");
        lore.add("§7NPC 매입: " + (e.sellable ? "§a가능" : "§c불가"));
        if (!e.jobs.isEmpty()) {
            lore.add("§7직업: §f" + String.join("§7, §f", e.jobsDisplay()));
        }
        lore.add("§7등급 적용: §f고급 x" + formatMultiplier(this.multRare) + " §7/ §f최고급 x" + formatMultiplier(this.multEpic));
        lore.add("§8클릭 시 판매되지는 않습니다.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreviewInfoItem(SellResult r, int amount) {
        Material type = r.ok ? Material.EMERALD : Material.BARRIER;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (r.ok) {
            meta.setDisplayName("§a판매 미리보기");
            List<String> lore = new ArrayList<>();
            lore.add("§7아이템: §f" + r.name);
            lore.add("§7수량: §f" + amount);
            lore.add("§7총 판매가: §a" + formatMoney(r.total) + getCurrencySuffix());
            if (Math.abs(r.mult - 1.0) > 0.001) {
                lore.add("§7등급 배율: §fx" + formatMultiplier(r.mult));
            }
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§c매입 불가");
            List<String> lore = new ArrayList<>();
            if (r.name != null && !r.name.isBlank()) {
                lore.add("§7아이템: §f" + r.name);
            }
            lore.add("§7수량: §f" + amount);
            if (r.listedPrice >= 0L) {
                lore.add("§6기준가: §e" + formatMoney(r.listedPrice) + getCurrencySuffix());
                if (Math.abs(r.mult - 1.0) > 0.001) {
                    lore.add("§7등급 배율: §fx" + formatMultiplier(r.mult));
                }
            }
            lore.add("§7사유:");
            lore.add("§f" + r.reason);
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSellSlotPlaceholder() {
        ItemStack slot = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta sm = slot.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§7여기에 판매할 아이템");
            sm.setLore(Arrays.asList("§8(한 칸만 사용)", "§7아이템을 넣으면 가격이 표시됩니다."));
            slot.setItemMeta(sm);
        }
        return slot;
    }

    private ItemStack createPreviewPlaceholder() {
        return createButton(Material.NAME_TAG, "§e판매 미리보기", "§7판매할 아이템을 넣으면", "§7여기에 가격이 표시됩니다.");
    }

    private ItemStack createButton(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta pm = pane.getItemMeta();
        if (pm != null) {
            pm.setDisplayName(" ");
            pane.setItemMeta(pm);
        }
        return pane;
    }

    private boolean isPlaceholder(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        return item.getType().name().endsWith("_PANE");
    }

    private int getCatalogPageSize() {
        if (this.yml == null) return DEFAULT_PAGE_ITEM_SLOTS;
        int raw = this.yml.getInt("shop.gui.page_item_slots", DEFAULT_PAGE_ITEM_SLOTS);
        if (raw <= 0) return DEFAULT_PAGE_ITEM_SLOTS;
        return Math.min(45, raw);
    }

    private String getCurrencySuffix() {
        return this.yml == null ? "원" : this.yml.getString("shop.currency_suffix", "원");
    }

    private String formatMoney(long money) {
        return new DecimalFormat("#,###").format(money);
    }

    private String formatMultiplier(double value) {
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(value);
    }

    private String prettyMaterial(Material material) {
        if (material == null) return "알 수 없는 아이템";
        return prettyKey(material.name());
    }

    private String prettyKey(String raw) {
        if (raw == null || raw.isBlank()) return "알 수 없는 아이템";
        String[] split = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : split) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public static class SellHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class CatalogHolder implements InventoryHolder {
        private final int page;

        public CatalogHolder(int page) {
            this.page = page;
        }

        public int getPage() {
            return this.page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class ShopEntry {
        public final String key;
        public final String type;
        public final Material material;
        public final String itemId;
        public final long priceEach;
        public final String displayName;
        public final Material iconMaterial;
        public final List<String> jobs;
        public final String special;
        public final boolean sellable;

        private ShopEntry(String key, String type, Material material, String itemId, long priceEach, String displayName, Material iconMaterial, List<String> jobs, String special, boolean sellable) {
            this.key = key;
            this.type = type;
            this.material = material;
            this.itemId = itemId;
            this.priceEach = priceEach;
            this.displayName = displayName;
            this.iconMaterial = iconMaterial;
            this.jobs = jobs;
            this.special = special;
            this.sellable = sellable;
        }

        public static ShopEntry from(Map<?, ?> raw) {
            try {
                String key = String.valueOf(raw.get("key"));
                String type = String.valueOf(raw.get("type"));

                String mat = raw.get("material") == null ? null : String.valueOf(raw.get("material"));
                Material material = null;
                if (mat != null && !mat.isBlank()) {
                    try { material = Material.valueOf(mat); } catch (Exception ignored) {}
                }

                String itemId = raw.get("item_id") == null ? null : String.valueOf(raw.get("item_id"));
                long priceEach = Long.parseLong(String.valueOf(raw.get("price_each")));
                String display = raw.get("display") == null ? null : String.valueOf(raw.get("display"));

                String iconMat = raw.get("icon_material") == null ? null : String.valueOf(raw.get("icon_material"));
                Material iconMaterial = null;
                if (iconMat != null && !iconMat.isBlank()) {
                    try { iconMaterial = Material.valueOf(iconMat); } catch (Exception ignored) {}
                }

                List<String> jobs = new ArrayList<>();
                Object jobsRaw = raw.get("jobs");
                if (jobsRaw instanceof Iterable<?> iterable) {
                    for (Object o : iterable) {
                        if (o != null) jobs.add(String.valueOf(o));
                    }
                }

                String special = raw.get("special") == null ? null : String.valueOf(raw.get("special"));
                boolean sellable = raw.get("sell") == null || Boolean.parseBoolean(String.valueOf(raw.get("sell")));
                return new ShopEntry(key, type, material, itemId, priceEach, display, iconMaterial, jobs, special, sellable);
            } catch (Exception ex) {
                return null;
            }
        }

        public Material resolveIconMaterial() {
            if (this.iconMaterial != null) return this.iconMaterial;
            if ("material".equals(this.type) && this.material != null) return this.material;
            if ("special".equals(this.type) && "WATER_BOTTLE".equalsIgnoreCase(this.key)) return Material.POTION;
            return Material.BREAD;
        }

        public List<String> jobsDisplay() {
            List<String> out = new ArrayList<>();
            for (String job : this.jobs) {
                out.add(switch (job.toUpperCase()) {
                    case "MINER" -> JobService.koreanName(JobType.MINER);
                    case "FARMER" -> JobService.koreanName(JobType.FARMER);
                    case "COOK" -> JobService.koreanName(JobType.COOK);
                    case "FISHER" -> JobService.koreanName(JobType.FISHER);
                    case "HUNTER" -> JobService.koreanName(JobType.HUNTER);
                    case "BREWER" -> JobService.koreanName(JobType.BREWER);
                    default -> job;
                });
            }
            return out;
        }
    }

    public static class SellResult {
        public final boolean ok;
        public final String name;
        public final long total;
        public final double mult;
        public final String reason;
        public final long listedPrice;

        private SellResult(boolean ok, String name, long total, double mult, String reason, long listedPrice) {
            this.ok = ok;
            this.name = name;
            this.total = total;
            this.mult = mult;
            this.reason = reason;
            this.listedPrice = listedPrice;
        }

        public static SellResult sellable(String name, long total, double mult) {
            return new SellResult(true, name, total, mult, null, total);
        }

        public static SellResult pricedButNotSellable(String name, long listedPrice, double mult, String reason) {
            return new SellResult(false, name, 0L, mult, reason, listedPrice);
        }

        public static SellResult notSellable(String reason) {
            return new SellResult(false, null, 0L, 1.0, reason, -1L);
        }
    }

    private int enchantCoeff(org.bukkit.enchantments.Enchantment ench) {
        if (ench == null) return 0;
        String key = ench.getKey().getKey();
        List<String> c0 = this.plugin.getConfig().getStringList("economy.enchant.coeff_0");
        for (String s : c0) if (s != null && s.equalsIgnoreCase(key)) return 0;
        List<String> c6 = this.plugin.getConfig().getStringList("economy.enchant.coeff_6");
        for (String s : c6) if (s != null && s.equalsIgnoreCase(key)) return 6;
        return 3;
    }

    private long computeEnchantedBookPrice(ItemStack in, ItemMeta meta) {
        if (!(meta instanceof EnchantmentStorageMeta esm)) return -1;
        int unit = this.plugin.getConfig().getInt("economy.enchant.book_unit", 3000);
        long sum = 0;
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : esm.getStoredEnchants().entrySet()) {
            int coeff = enchantCoeff(e.getKey());
            if (coeff <= 0) continue;
            int lvl = e.getValue() == null ? 0 : e.getValue();
            sum += (long) coeff * (long) lvl * (long) unit;
        }
        return sum;
    }

    private long computeEnchantedGearPrice(ItemStack in, ItemMeta meta) {
        int unit = this.plugin.getConfig().getInt("economy.enchant.gear_unit", 1000);
        long base = this.plugin.getConfig().getLong("economy.base_prices." + in.getType().name().toLowerCase(java.util.Locale.ROOT), 0L);

        long enchSum = 0;
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : meta.getEnchants().entrySet()) {
            int coeff = enchantCoeff(e.getKey());
            if (coeff <= 0) continue;
            int lvl = e.getValue() == null ? 0 : e.getValue();
            enchSum += (long) coeff * (long) lvl * (long) unit;
        }

        long pre = base + enchSum;
        if (meta instanceof org.bukkit.inventory.meta.Damageable dmg && in.getType().getMaxDurability() > 0) {
            int max = in.getType().getMaxDurability();
            int cur = Math.max(0, max - dmg.getDamage());
            double ratio = (double) cur / (double) max;
            pre = Math.round(pre * ratio);
        }
        return pre;
    }
}
