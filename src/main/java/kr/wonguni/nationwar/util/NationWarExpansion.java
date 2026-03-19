/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package kr.wonguni.nationwar.util;

import java.lang.reflect.Field;
import kr.wonguni.nationwar.NationWarPlugin;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.model.StatType;
import kr.wonguni.nationwar.service.MoneyService;
import kr.wonguni.nationwar.service.StatService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NationWarExpansion
extends PlaceholderExpansion {
    private final NationWarPlugin plugin;
    private MoneyService moneyService;
    private StatService statService;

    public NationWarExpansion(NationWarPlugin plugin) {
        this.plugin = plugin;
        try {
            Field moneyField = ((Object)((Object)plugin)).getClass().getDeclaredField("moneyService");
            moneyField.setAccessible(true);
            this.moneyService = (MoneyService)moneyField.get((Object)plugin);
            Field statField = ((Object)((Object)plugin)).getClass().getDeclaredField("statService");
            statField.setAccessible(true);
            this.statService = (StatService)statField.get((Object)plugin);
        }
        catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize NationWarExpansion fields.");
        }
    }

    @NotNull
    public String getAuthor() {
        return "Manus";
    }

    @NotNull
    public String getIdentifier() {
        return "nationwar";
    }

    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        if (params.equalsIgnoreCase("money")) {
            return String.valueOf(this.moneyService.getBalance(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("hunger_reserve")) {
            PlayerProfile prof = this.statService.getProfile(player.getUniqueId());
            return String.valueOf(prof.getHungerReserve());
        }
        if (params.equalsIgnoreCase("hunger_max")) {
            PlayerProfile prof = this.statService.getProfile(player.getUniqueId());
            return String.valueOf(prof.getStat(StatType.HUNGER));
        }
        if (params.equalsIgnoreCase("combat_level")) {
            PlayerProfile prof = this.statService.getProfile(player.getUniqueId());
            return String.valueOf(prof.getCombatLevel());
        }
        if (params.equalsIgnoreCase("int_health")) {
            if (player instanceof Player) {
                Player onlinePlayer = (Player)player;
                return String.valueOf((int)Math.ceil(onlinePlayer.getHealth()));
            }
            return "0";
        }
        if (params.equalsIgnoreCase("int_max_health")) {
            if (player instanceof Player) {
                Player onlinePlayer = (Player)player;
                return String.valueOf((int)Math.ceil(onlinePlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            }
            return "20";
        }
        if (params.equalsIgnoreCase("total_food")) {
            if (player instanceof Player) {
                Player onlinePlayer = (Player)player;
                PlayerProfile prof = this.statService.getProfile(player.getUniqueId());
                int hungerStat = prof.getStat(StatType.HUNGER);
                int reserve = Math.max(0, prof.getHungerReserve());
                int food = onlinePlayer.getFoodLevel();
                if (food == 19 && reserve > 0 && hungerStat > 0) {
                    return String.valueOf(20 + reserve);
                }
                return String.valueOf(food + reserve);
            }
            return "20";
        }
        if (params.equalsIgnoreCase("max_food")) {
            PlayerProfile prof = this.statService.getProfile(player.getUniqueId());
            int hungerStat = prof.getStat(StatType.HUNGER);
            return String.valueOf(20 + hungerStat);
        }
        return null;
    }
}

