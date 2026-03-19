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

import kr.wonguni.nationwar.service.MoneyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand
implements CommandExecutor {
    private final MoneyService money;

    public MoneyCommand(MoneyService money) {
        this.money = money;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("take"))) {
            long amount;
            if (!sender.hasPermission("nationwar.admin") && !sender.isOp()) {
                sender.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /money set <player> <amount>");
                sender.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /money add <player> <amount>");
                sender.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /money take <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayerExact((String)args[1]);
            if (target == null) {
                sender.sendMessage("\u00a7c\ub300\uc0c1 \ud50c\ub808\uc774\uc5b4\ub294 \uc628\ub77c\uc778\uc774\uc5b4\uc57c \ud569\ub2c8\ub2e4: " + args[1]);
                return true;
            }
            try {
                amount = Long.parseLong(args[2]);
            }
            catch (NumberFormatException ex) {
                sender.sendMessage("\u00a7c\uc22b\uc790\uac00 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4: " + args[2]);
                return true;
            }
            if (amount < 0L) {
                amount = 0L;
            }
            if (args[0].equalsIgnoreCase("set")) {
                this.money.setBalance(target.getUniqueId(), amount);
                sender.sendMessage("\u00a7a[\ub3c8] \u00a7f\uc124\uc815 \uc644\ub8cc: " + target.getName() + " =" + amount);
                target.sendMessage("\u00a7e[\ub3c8] \u00a7f\uad00\ub9ac\uc790\uac00 \ubcf4\uc720\uae08\uc744 \u00a7a" + amount + "\u00a7f(\uc73c)\ub85c \uc124\uc815\ud588\uc2b5\ub2c8\ub2e4.");
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                this.money.deposit(target.getUniqueId(), amount);
                sender.sendMessage("\u00a7a[\ub3c8] \u00a7f\uc9c0\uae09 \uc644\ub8cc: " + target.getName() + " +" + amount);
                target.sendMessage("\u00a7e[\ub3c8] \u00a7f\uad00\ub9ac\uc790\uac00 \ubcf4\uc720\uae08 \u00a7a" + amount + "\u00a7f\uc744(\ub97c) \uc9c0\uae09\ud588\uc2b5\ub2c8\ub2e4.");
                return true;
            }
            if (args[0].equalsIgnoreCase("take")) {
                boolean ok = this.money.withdraw(target.getUniqueId(), amount);
                if (!ok) {
                    sender.sendMessage("\u00a7c[\ub3c8] \u00a7f\ucc28\uac10 \uc2e4\ud328(\uc794\uc561 \ubd80\uc871). \ud604\uc7ac: " + this.money.getBalance(target.getUniqueId()));
                    return true;
                }
                sender.sendMessage("\u00a7a[\ub3c8] \u00a7f\ucc28\uac10 \uc644\ub8cc: " + target.getName() + " -" + amount);
                target.sendMessage("\u00a7e[\ub3c8] \u00a7f\uad00\ub9ac\uc790\uac00 \ubcf4\uc720\uae08 \u00a7c" + amount + "\u00a7f\uc744(\ub97c) \ucc28\uac10\ud588\uc2b5\ub2c8\ub2e4.");
                return true;
            }
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("\ud50c\ub808\uc774\uc5b4\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        Player p = (Player)sender;
        this.money.notifyBalance(p);
        return true;
    }
}

