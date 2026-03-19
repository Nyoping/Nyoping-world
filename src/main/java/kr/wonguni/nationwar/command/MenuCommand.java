/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package kr.wonguni.nationwar.command;

import kr.wonguni.nationwar.service.GuiService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand
implements CommandExecutor {
    private final GuiService gui;

    public MenuCommand(GuiService gui) {
        this.gui = gui;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\ud50c\ub808\uc774\uc5b4\ub9cc \uc0ac\uc6a9\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.");
            return true;
        }
        Player p = (Player)sender;
        this.gui.openPlayerMenu(p);
        return true;
    }
}

