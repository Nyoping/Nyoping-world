/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.command;

import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.service.BorderVisualService;
import kr.wonguni.nationwar.service.NationService;
import kr.wonguni.nationwar.service.WarService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NationCommand
implements CommandExecutor {
    private final JavaPlugin plugin;
    private final NationService nation;
    private final BorderVisualService borderVisual;
    private final WarService war;

    public NationCommand(JavaPlugin plugin, NationService nation, BorderVisualService borderVisual, WarService war) {
        this.plugin = plugin;
        this.nation = nation;
        this.borderVisual = borderVisual;
        this.war = war;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (!(sender instanceof Player)) {
            sender.sendMessage("\ud50c\ub808\uc774\uc5b4\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        Player p = (Player)sender;
        if (args.length == 0) {
            this.sendHelp(p);
            return true;
        }
        switch (sub = args[0].toLowerCase()) {
            case "help": {
                this.sendHelp(p);
                break;
            }
            case "invite": {
                if (args.length < 2) {
                    p.sendMessage("\u00a7c/nation invite <\ud50c\ub808\uc774\uc5b4>");
                    return true;
                }
                OfflinePlayer t = Bukkit.getOfflinePlayer((String)args[1]);
                this.nation.invite(p, t);
                break;
            }
            case "accept": {
                this.nation.acceptInvite(p);
                break;
            }
            case "decline": {
                this.nation.declineInvite(p);
                break;
            }
            case "leave": {
                this.nation.leaveNation(p);
                break;
            }
            case "disband": {
                Nation n = this.nation.getNationOf(p.getUniqueId());
                if (n == null) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                if (!p.getUniqueId().equals(n.getLeaderUuid())) {
                    p.sendMessage("\u00a7c\uad6d\uac00\uc7a5\ub9cc \ud574\uccb4\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                int removed = this.war.removeNationStructures(n.getName());
                boolean ok = this.nation.disband(p);
                if (!ok) break;
                p.sendMessage("\u00a7c[\uad6d\uac00] \u00a7f\uad6d\uac00\ub97c \ud574\uccb4\ud588\uc2b5\ub2c8\ub2e4. (\uad6c\uc870\ubb3c \uc81c\uac70 " + removed + "\uac1c)");
                break;
            }
            case "upkeepreset": {
                if (!p.hasPermission("nationwar.admin")) {
                    p.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("\u00a7c/nation upkeepreset <\uad6d\uac00\uc774\ub984>");
                    return true;
                }
                String name = args[1];
                boolean ok = this.nation.adminResetUpkeep(name);
                if (ok) {
                    p.sendMessage("\u00a7a[\uad6d\uac00] \u00a7f\uc720\uc9c0\uae08(\uccb4\ub0a9) \uc0c1\ud0dc\ub97c \ucd08\uae30\ud654\ud588\uc2b5\ub2c8\ub2e4: \u00a7e" + name);
                    break;
                }
                p.sendMessage("\u00a7c\ud574\ub2f9 \uad6d\uac00\uac00 \uc5c6\uc2b5\ub2c8\ub2e4: " + name);
                break;
            }
            case "deposit": {
                long amt;
                if (args.length < 2) {
                    p.sendMessage("\u00a7c/nation deposit <\uae08\uc561>");
                    return true;
                }
                try {
                    amt = Long.parseLong(args[1]);
                }
                catch (NumberFormatException e) {
                    p.sendMessage("\u00a7c\uae08\uc561\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                this.nation.depositToTreasury(p, amt);
                break;
            }
            case "withdraw": {
                long amt;
                if (!p.hasPermission("nationwar.admin")) {
                    p.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                if (args.length < 4) {
                    p.sendMessage("\u00a7c/nation withdraw <\uad6d\uac00\uc774\ub984> <\ud50c\ub808\uc774\uc5b4> <\uae08\uc561>");
                    return true;
                }
                String nn = args[1];
                OfflinePlayer t = Bukkit.getOfflinePlayer((String)args[2]);
                try {
                    amt = Long.parseLong(args[3]);
                }
                catch (NumberFormatException e) {
                    p.sendMessage("\u00a7c\uae08\uc561\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                boolean ok = this.nation.adminWithdrawFromTreasury(nn, t.getUniqueId(), amt);
                if (ok) {
                    p.sendMessage("\u00a7a[\uad6d\uace0] \u00a7f" + nn + " \uad6d\uace0\uc5d0\uc11c " + amt + " \ucd9c\uae08\ud558\uc5ec " + String.valueOf(t.getName() != null ? t.getName() : t.getUniqueId()) + "\uc5d0\uac8c \uc9c0\uae09\ud588\uc2b5\ub2c8\ub2e4.");
                    break;
                }
                p.sendMessage("\u00a7c\uc2e4\ud328: \uad6d\uac00\uac00 \uc5c6\uac70\ub098 \uad6d\uace0\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4.");
                break;
            }
            case "upgrade": {
                this.nation.upgrade(p);
                break;
            }
            case "info": {
                Nation n = this.nation.getNationOf(p.getUniqueId());
                if (n == null) {
                    p.sendMessage("\u00a7e[\uad6d\uac00] \u00a7f\ubb34\uc18c\uc18d");
                    return true;
                }
                p.sendMessage("\u00a7e[\uad6d\uac00] \u00a7f" + n.getName() + " \u00a77(Lv." + n.getLevel() + ")");
                p.sendMessage("\u00a77\uad6d\uace0: \u00a7a" + n.getTreasury() + " \u00a77\uccb4\ub0a9\uc77c: " + n.getArrearsDays());
                p.sendMessage("\u00a77\ub125\uc11c\uc2a4: " + (String)(n.hasNexus() ? n.getNexusWorld() + " (" + n.getNexusX() + "," + n.getNexusY() + "," + n.getNexusZ() + ")" : "\uc5c6\uc74c"));
                break;
            }
            case "tp": 
            case "home": {
                Nation n = this.nation.getNationOf(p.getUniqueId());
                if (n == null || !n.hasNexus()) {
                    p.sendMessage("\u00a7c\uad6d\uac00 \ub125\uc11c\uc2a4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                if (n.getNexusWorld() == null) {
                    p.sendMessage("\u00a7c\uad6d\uac00 \ub125\uc11c\uc2a4 \uc6d4\ub4dc \uc815\ubcf4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                World w = Bukkit.getWorld((String)n.getNexusWorld());
                if (w == null) {
                    p.sendMessage("\u00a7c\ub125\uc11c\uc2a4 \uc6d4\ub4dc\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4: " + n.getNexusWorld());
                    return true;
                }
                Location loc = new Location(w, (double)n.getNexusX() + 0.5, (double)n.getNexusY() + 1.0, (double)n.getNexusZ() + 0.5);
                loc = w.getHighestBlockAt(loc).getLocation().add(0.5, 1.0, 0.5);
                p.teleport(loc);
                p.sendMessage("\u00a7a[\uad6d\uac00] \u00a7f\uad6d\uac00 \ub125\uc11c\uc2a4\ub85c \uc774\ub3d9\ud588\uc2b5\ub2c8\ub2e4.");
                break;
            }
            case "border": {
                int secs = this.plugin.getConfig().getInt("ui.border-visual.default-seconds", 15);
                if (args.length >= 2) {
                    try {
                        secs = Integer.parseInt(args[1]);
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                }
                this.borderVisual.showOwnNationBorder(p, Math.max(3, Math.min(secs, 120)));
                break;
            }
            default: {
                this.sendHelp(p);
            }
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage("\u00a7e/nation invite <\ud50c\ub808\uc774\uc5b4>");
        p.sendMessage("\u00a7e/nation accept | decline");
        p.sendMessage("\u00a7e/nation deposit <\uae08\uc561>  \u00a77(\uad6d\uace0 \uc785\uae08)");
        if (p.hasPermission("nationwar.admin")) {
            p.sendMessage("\u00a7e/nation withdraw <\uad6d\uac00\uc774\ub984> <\ud50c\ub808\uc774\uc5b4> <\uae08\uc561>  \u00a77(\uad00\ub9ac\uc790: \uad6d\uace0 \ucd9c\uae08)");
        }
        p.sendMessage("\u00a7e/nation upgrade  \u00a77(\uad6d\uac00 \ub808\ubca8 \uc5c5\uadf8\ub808\uc774\ub4dc)");
        p.sendMessage("\u00a7e/nation info");
        p.sendMessage("\u00a7e/nation tp \u00a77(\uad6d\uac00 \ub125\uc11c\uc2a4\ub85c \uc774\ub3d9)");
        p.sendMessage("\u00a7e/nation border [\ucd08] \u00a77(\ubcf4\ud638\uad6c\uc5ed \uacbd\uacc4 \ud45c\uc2dc)");
        p.sendMessage("\u00a7e/nation leave | disband");
    }
}

