/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package kr.wonguni.nationwar.command;

import kr.wonguni.nationwar.service.BindService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BindCommand
implements CommandExecutor {
    private final BindService bindService;
    private final boolean absolute;

    public BindCommand(BindService bindService, boolean absolute) {
        this.bindService = bindService;
        this.absolute = absolute;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\ud50c\ub808\uc774\uc5b4\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        Player p = (Player)sender;
        if (!p.hasPermission("nationwar.bind")) {
            p.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage("\u00a7c\uc0ac\uc6a9\ubc95: /" + label + " <\ud50c\ub808\uc774\uc5b4>");
            return true;
        }
        Player target = Bukkit.getPlayerExact((String)args[0]);
        if (target == null) {
            p.sendMessage("\u00a7c\ud574\ub2f9 \ud50c\ub808\uc774\uc5b4\uac00 \uc628\ub77c\uc778\uc774 \uc544\ub2d9\ub2c8\ub2e4.");
            return true;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            p.sendMessage("\u00a7c\uc190\uc5d0 \uc544\uc774\ud15c\uc744 \ub4e4\uc5b4\uc8fc\uc138\uc694.");
            return true;
        }
        if (!this.bindService.bind(hand, target.getUniqueId(), this.absolute)) {
            p.sendMessage("\u00a7c\uadc0\uc18d \ucc98\ub9ac\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        p.sendMessage("\u00a7a\uc544\uc774\ud15c\uc744 " + (this.absolute ? "\uc808\ub300\uadc0\uc18d" : "\uadc0\uc18d") + " \ucc98\ub9ac\ud588\uc2b5\ub2c8\ub2e4: " + target.getName());
        return true;
    }
}

