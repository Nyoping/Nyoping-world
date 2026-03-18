/*
 * NationWar - Shop Command
 */
package kr.wonguni.nationwar.command;

import kr.wonguni.nationwar.service.ShopService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    private final ShopService shop;

    public ShopCommand(ShopService shop) {
        this.shop = shop;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nationwar.admin")) {
                sender.sendMessage("§c권한이 없습니다.");
                return true;
            }
            boolean ok = this.shop.reload();
            sender.sendMessage(ok ? "§a[상점] shop.yml 리로드 완료" : "§c[상점] shop.yml 리로드 실패(콘솔 확인)");
            return true;
        }

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("sell")) {
            this.shop.openSellGui(p);
            return true;
        }

        this.shop.openCatalogGui(p);
        return true;
    }
}
