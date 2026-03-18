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

public class PayCommand
implements CommandExecutor {
    private final MoneyService money;

    public PayCommand(MoneyService money) {
        this.money = money;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        long amount;
        if (!(sender instanceof Player)) {
            sender.sendMessage("\ud50c\ub808\uc774\uc5b4\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        Player p = (Player)sender;
        if (args.length < 2) {
            p.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /pay <\ud50c\ub808\uc774\uc5b4> <\uae08\uc561>");
            return true;
        }
        Player target = Bukkit.getPlayerExact((String)args[0]);
        if (target == null) {
            p.sendMessage("\u00a7c\ud574\ub2f9 \ud50c\ub808\uc774\uc5b4\uac00 \uc628\ub77c\uc778\uc774 \uc544\ub2d9\ub2c8\ub2e4.");
            return true;
        }
        try {
            amount = Long.parseLong(args[1]);
        }
        catch (NumberFormatException e) {
            p.sendMessage("\u00a7c\uae08\uc561\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        if (amount <= 0L) {
            p.sendMessage("\u00a7c0\ubcf4\ub2e4 \ud070 \uae08\uc561\ub9cc \uac00\ub2a5\ud569\ub2c8\ub2e4.");
            return true;
        }
        if (!this.money.transfer(p.getUniqueId(), target.getUniqueId(), amount)) {
            p.sendMessage("\u00a7c\ub3c8\uc774 \ubd80\uc871\ud569\ub2c8\ub2e4.");
            return true;
        }
        p.sendMessage("\u00a7a" + target.getName() + "\u00a7f\uc5d0\uac8c \u00a7a" + amount + "\u00a7f \uc9c0\uae09 \uc644\ub8cc.");
        target.sendMessage("\u00a7e" + p.getName() + "\u00a7f\ub2d8\uc5d0\uac8c \u00a7a" + amount + "\u00a7f \ubc1b\uc558\uc2b5\ub2c8\ub2e4.");
        return true;
    }
}

