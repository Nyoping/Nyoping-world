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
import kr.wonguni.nationwar.service.NationService;
import kr.wonguni.nationwar.service.WarService;
import kr.wonguni.nationwar.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NexusCommand
implements CommandExecutor {
    private final WarService warService;
    private final NationService nationService;
    private final BindService bindService;

    public NexusCommand(WarService warService, NationService nationService, BindService bindService) {
        this.warService = warService;
        this.nationService = nationService;
        this.bindService = bindService;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nationwar.nexus")) {
            sender.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                sender.sendMessage("\u00a7e/nexus give <player>");
                return true;
            }
            Player t = Bukkit.getPlayerExact((String)args[1]);
            if (t == null) {
                sender.sendMessage("\u00a7c\uc628\ub77c\uc778 \ud50c\ub808\uc774\uc5b4\ub9cc \uc9c0\uae09\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
                return true;
            }
            ItemStack item = Items.createNexusItem(this.warService.getPlugin());
            this.bindService.bind(item, t.getUniqueId(), true);
            t.getInventory().addItem(new ItemStack[]{item});
            sender.sendMessage("\u00a7a\ub125\uc11c\uc2a4 \uc544\uc774\ud15c \uc9c0\uae09 \uc644\ub8cc: " + t.getName());
            return true;
        }
        sender.sendMessage("\u00a7e/nexus give <player>");
        return true;
    }
}

