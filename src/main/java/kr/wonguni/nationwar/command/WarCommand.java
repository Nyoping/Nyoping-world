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

import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.service.WarService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarCommand
implements CommandExecutor {
    private final WarService war;

    public WarCommand(WarService war) {
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
            p.sendMessage("\u00a7e/war challenge <\uad6d\uac00\uba85>");
            p.sendMessage("\u00a7e/war accept | decline");
            p.sendMessage("\u00a7e/war status");
            p.sendMessage("\u00a7e/war forcestart <\uad6d\uac00A> <\uad6d\uac00B> \u00a77(\uad00\ub9ac\uc790/OP)");
            return true;
        }
        switch (sub = args[0].toLowerCase()) {
            case "challenge": {
                if (args.length < 2) {
                    p.sendMessage("\u00a7c/war challenge <\uad6d\uac00\uba85>");
                    return true;
                }
                Nation target = this.war.getStore().getNationByName(args[1]);
                if (target == null) {
                    p.sendMessage("\u00a7c\ud574\ub2f9 \uad6d\uac00\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                this.war.startChallenge(p, target);
                break;
            }
            case "accept": {
                this.war.accept(p);
                break;
            }
            case "decline": {
                this.war.decline(p);
                break;
            }
            case "status": {
                this.war.status(p);
                break;
            }
            case "forcestart": {
                if (!p.isOp() && !p.hasPermission("nationwar.admin")) {
                    p.sendMessage("\u00a7c\uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.");
                    return true;
                }
                if (args.length < 3) {
                    p.sendMessage("\u00a7c/war forcestart <\uad6d\uac00A> <\uad6d\uac00B>");
                    return true;
                }
                boolean ok = this.war.forceStart(args[1], args[2]);
                if (ok) {
                    p.sendMessage("\u00a7a[\uc804\uc7c1] \uac15\uc81c \uc2dc\uc791\ud588\uc2b5\ub2c8\ub2e4.");
                    break;
                }
                p.sendMessage("\u00a7c\uad6d\uac00\ub97c \ucc3e\uc744 \uc218 \uc5c6\uac70\ub098 \uc2dc\uc791 \uc2e4\ud328.");
                break;
            }
            default: {
                p.sendMessage("\u00a7e/war challenge <\uad6d\uac00\uba85>");
                p.sendMessage("\u00a7e/war accept | decline");
                p.sendMessage("\u00a7e/war status");
                p.sendMessage("\u00a7e/war forcestart <\uad6d\uac00A> <\uad6d\uac00B> \u00a77(\uad00\ub9ac\uc790/OP)");
            }
        }
        return true;
    }
}

