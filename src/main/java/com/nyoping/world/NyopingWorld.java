package com.nyoping.world;

import com.nyoping.world.commands.*;
import com.nyoping.world.listeners.ChatListener;
import com.nyoping.world.listeners.PlayerJoinListener;
import com.nyoping.world.managers.EconomyManager;
import com.nyoping.world.managers.RankManager;
import com.nyoping.world.managers.TeleportManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NyopingWorld extends JavaPlugin {

    private static NyopingWorld instance;
    private RankManager rankManager;
    private EconomyManager economyManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 매니저 초기화
        rankManager = new RankManager(this);
        economyManager = new EconomyManager(this);
        teleportManager = new TeleportManager(this);

        // 커맨드 등록
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("delwarp").setExecutor(new DelWarpCommand(this));
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this));
        getCommand("broadcast").setExecutor(new BroadcastCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("NyopingWorld 플러그인이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) economyManager.saveAll();
        if (rankManager != null) rankManager.saveAll();
        if (teleportManager != null) teleportManager.saveAll();
        getLogger().info("NyopingWorld 플러그인이 비활성화되었습니다.");
    }

    public static NyopingWorld getInstance() {
        return instance;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
