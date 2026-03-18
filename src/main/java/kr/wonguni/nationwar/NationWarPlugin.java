package kr.wonguni.nationwar;

import kr.wonguni.nationwar.command.*;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.service.*;
import kr.wonguni.nationwar.util.NationWarExpansion;
import kr.wonguni.nationwar.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NationWarPlugin extends JavaPlugin {
    private DataStore dataStore;
    private MoneyService moneyService;
    private NationService nationService;
    private BindService bindService;
    private JobService jobService;
    private JobQuestService jobQuestService;
    private StatService statService;
    private WarService warService;
    private ProtectionService protectionService;
    private HudService hudService;
    private BorderVisualService borderVisualService;
    private GuiService guiService;
    private CustomCropService customCropService;
    private CustomFoodService customFoodService;
    private WaterService waterService;
    private ShopService shopService;
    private ChestLockService chestLockService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.dataStore = new DataStore(this);
        this.dataStore.load();
        this.moneyService = new MoneyService(this, this.dataStore);
        this.nationService = new NationService(this, this.dataStore, this.moneyService);
        this.bindService = new BindService(this);
        this.jobService = new JobService(this, this.dataStore);
        this.jobQuestService = new JobQuestService(this);
        this.statService = new StatService(this, this.dataStore);
        this.protectionService = new ProtectionService(this, this.nationService);
        this.chestLockService = new ChestLockService(this, this.nationService);
        this.warService = new WarService(this, this.dataStore, this.nationService, this.moneyService, this.protectionService);
        this.hudService = new HudService(this, this.dataStore, this.moneyService, this.nationService, this.jobService);
        this.borderVisualService = new BorderVisualService(this, this.nationService);
        this.guiService = new GuiService(this.jobService, this.statService, this.nationService, this.warService);
        this.customCropService = new CustomCropService(this);
        this.customFoodService = new CustomFoodService(this);
        this.waterService = new WaterService(this);
        this.shopService = new ShopService(this, this.moneyService, this.jobService);

        Bukkit.getPluginManager().registerEvents(this.guiService, this);
        Bukkit.getPluginManager().registerEvents(new ShopListener(this.shopService), this);
        Bukkit.getPluginManager().registerEvents(new JobQuestListener(this.dataStore, this.jobQuestService), this);
        Bukkit.getPluginManager().registerEvents(new HunterMobLevelListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CookWorldRulesListener(this, this.jobService), this);
        Bukkit.getPluginManager().registerEvents(new CookTriggerListener(this, this.jobService, this.customFoodService), this);
        Bukkit.getPluginManager().registerEvents(new WaterListener(this, this.waterService), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, this.moneyService, this.nationService, this.bindService, this.jobService, this.protectionService, this.warService, this.borderVisualService, this.guiService), this);
        Bukkit.getPluginManager().registerEvents(new HunterListener(this, this.dataStore, this.jobService), this);
        Bukkit.getPluginManager().registerEvents(new CookListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CustomCropListener(this.dataStore, this.customCropService), this);
        Bukkit.getPluginManager().registerEvents(new FarmerListener(this, this.dataStore, this.jobService, this.customCropService), this);
        Bukkit.getPluginManager().registerEvents(new MinerListener(this, this.dataStore, this.jobService), this);
        Bukkit.getPluginManager().registerEvents(new FisherListener(this, this.dataStore, this.jobService), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this.protectionService), this);
        Bukkit.getPluginManager().registerEvents(new BindListener(this, this.bindService), this);
        Bukkit.getPluginManager().registerEvents(new ChestLockListener(this, this.nationService, this.protectionService, this.chestLockService), this);
        Bukkit.getPluginManager().registerEvents(new WarListener(this.warService), this);

        if (getCommand("menu") != null) getCommand("menu").setExecutor(new MenuCommand(this.guiService));
        if (getCommand("nation") != null) getCommand("nation").setExecutor(new NationCommand(this, this.nationService, this.borderVisualService, this.warService));
        if (getCommand("war") != null) getCommand("war").setExecutor(new WarCommand(this.warService));
        if (getCommand("money") != null) getCommand("money").setExecutor(new MoneyCommand(this.moneyService));
        if (getCommand("pay") != null) getCommand("pay").setExecutor(new PayCommand(this.moneyService));
        if (getCommand("bind") != null) getCommand("bind").setExecutor(new BindCommand(this.bindService, false));
        if (getCommand("abind") != null) getCommand("abind").setExecutor(new BindCommand(this.bindService, true));
        if (getCommand("nexus") != null) getCommand("nexus").setExecutor(new NexusCommand(this.warService, this.nationService, this.bindService));
        if (getCommand("stat") != null) getCommand("stat").setExecutor(new StatCommand(this.statService));
        if (getCommand("shop") != null) getCommand("shop").setExecutor(new ShopCommand(this.shopService));
        if (getCommand("jobadmin") != null) getCommand("jobadmin").setExecutor(new JobAdminCommand(this.dataStore, this.jobQuestService));
        if (getCommand("givecropseed") != null) getCommand("givecropseed").setExecutor(new GiveCropSeedCommand(this.customCropService));
        if (getCommand("givecookfood") != null) getCommand("givecookfood").setExecutor(new GiveCookFoodCommand(this.customFoodService));

        this.jobQuestService.ensureDatapack();
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(player -> this.jobQuestService.syncPlayer(player, this.dataStore.getOrCreatePlayer(player.getUniqueId()))), 20L);
        this.moneyService.start();
        this.nationService.startDailyUpkeepScheduler();
        this.warService.start();
        this.hudService.start();

        getLogger().info("NationWar enabled. Next upkeep at " + String.valueOf(TimeUtil.nextKstMidnight()));
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NationWarExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    @Override
    public void onDisable() {
        try {
            if (this.hudService != null) this.hudService.stop();
        } catch (Exception ignored) {}
        try {
            if (this.dataStore != null) this.dataStore.saveAll();
        } catch (Exception ignored) {}
        try {
            if (this.waterService != null) this.waterService.save();
        } catch (Exception ignored) {}
    }

    public JobQuestService getJobQuestService() {
        return this.jobQuestService;
    }
}
