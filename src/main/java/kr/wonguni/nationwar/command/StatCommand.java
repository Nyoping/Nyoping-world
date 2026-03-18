/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package kr.wonguni.nationwar.command;

import java.util.Map;
import kr.wonguni.nationwar.model.StatType;
import kr.wonguni.nationwar.service.StatService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatCommand
implements CommandExecutor {
    private final StatService stat;

    public StatCommand(StatService stat) {
        this.stat = stat;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("set"))) {
            int amount;
            if (!sender.hasPermission("nationwar.admin")) {
                sender.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /stat give <player> <points>");
                sender.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /stat take <player> <points>");
                sender.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /stat set <player> <points>");
                return true;
            }
            Player target = Bukkit.getPlayerExact((String)args[1]);
            if (target == null) {
                sender.sendMessage("\u00a7c\ub300\uc0c1 \ud50c\ub808\uc774\uc5b4\ub294 \uc628\ub77c\uc778\uc774\uc5b4\uc57c \ud569\ub2c8\ub2e4: " + args[1]);
                return true;
            }
            try {
                amount = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException ex) {
                sender.sendMessage("\u00a7c\uc22b\uc790\uac00 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4: " + args[2]);
                return true;
            }
            if (amount < 0) {
                amount = 0;
            }
            if (args[0].equalsIgnoreCase("give")) {
                this.stat.adminAddPoints(target, amount);
                sender.sendMessage("\u00a7a[\uc2a4\ud0ef] \u00a7f\uc9c0\uae09 \uc644\ub8cc: " + target.getName() + " +" + amount);
                return true;
            }
            if (args[0].equalsIgnoreCase("take")) {
                this.stat.adminAddPoints(target, -amount);
                sender.sendMessage("\u00a7a[\uc2a4\ud0ef] \u00a7f\ucc28\uac10 \uc644\ub8cc: " + target.getName() + " -" + amount);
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                this.stat.adminSetPoints(target, amount);
                sender.sendMessage("\u00a7a[\uc2a4\ud0ef] \u00a7f\uc124\uc815 \uc644\ub8cc: " + target.getName() + " =" + amount);
                return true;
            }
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("\ud50c\ub808\uc774\uc5b4\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4. (\uad00\ub9ac\uc790\ub294 /stat give|take|set \uc0ac\uc6a9)");
            return true;
        }
        Player p = (Player)sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            int points = this.stat.getStatPoints(p.getUniqueId());
            Map<StatType, Integer> m = this.stat.getStats(p.getUniqueId());
            p.sendMessage("\u00a7a[\uc2a4\ud0ef] \u00a7f\ub0a8\uc740 \uc2a4\ud0ef\ud3ec\uc778\ud2b8: \u00a7e" + points);
            StringBuilder sb = new StringBuilder("\u00a7a[\uc2a4\ud0ef] \u00a7f");
            boolean first = true;
            for (StatType t : StatType.values()) {
                if (!first) {
                    sb.append("  ");
                }
                first = false;
                sb.append(t.koreanName()).append(":\u00a7e").append(m.getOrDefault((Object)t, 0));
            }
            p.sendMessage(sb.toString());
            p.sendMessage("\u00a7a[\uc2a4\ud0ef] \u00a7f\uac15\ud654: \u00a7e/stat up <\uacf5\uaca9|\ud5c8\uae30|\uccb4\ub825>");
            return true;
        }
        if (args[0].equalsIgnoreCase("up") || args[0].equalsIgnoreCase("upgrade")) {
            if (args.length < 2) {
                p.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /stat up <\uacf5\uaca9|\ud5c8\uae30|\uccb4\ub825>");
                return true;
            }
            StatType type = StatType.fromString(args[1]);
            if (type == null) {
                p.sendMessage("\u00a7c\uc54c \uc218 \uc5c6\ub294 \uc2a4\ud0ef\uc785\ub2c8\ub2e4. \uc0ac\uc6a9 \uac00\ub2a5: \uacf5\uaca9/\ud5c8\uae30/\uccb4\ub825");
                return true;
            }
            boolean ok = this.stat.upgrade(p, type);
            if (!ok) {
                p.sendMessage("\u00a7c[\uc2a4\ud0ef] \u00a7f\uc2a4\ud0ef\ud3ec\uc778\ud2b8\uac00 \ubd80\uc871\ud569\ub2c8\ub2e4.");
                return true;
            }
            p.sendMessage("\u00a7a[\uc2a4\ud0ef] \u00a7f" + type.koreanName() + " \uac15\ud654! \u00a77(\uc804\ud22c\ub808\ubca8 +1)");
            return true;
        }
        p.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /stat, /stat info, /stat up <\uacf5\uaca9|\ud5c8\uae30|\uccb4\ub825>");
        p.sendMessage("\u00a77(OP) /stat give|take|set <player> <points>");
        return true;
    }
}

