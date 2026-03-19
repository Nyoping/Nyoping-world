package kr.wonguni.nationwar.command;

import kr.wonguni.nationwar.model.CustomCropType;
import kr.wonguni.nationwar.service.CustomCropService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveCropSeedCommand implements CommandExecutor {
    private final CustomCropService crops;

    public GiveCropSeedCommand(CustomCropService crops) {
        this.crops = crops;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("player only");
            return true;
        }
        if (!sender.hasPermission("nationwar.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§e사용법: /givecropseed <type> [amount]");
            return true;
        }
        CustomCropType type = CustomCropType.fromId(args[0]);
        if (type == null) {
            sender.sendMessage("§c알 수 없는 작물 타입");
            return true;
        }
        int amount = 16;
        if (args.length >= 2) {
            try { amount = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        }
        p.getInventory().addItem(this.crops.seedItem(type, Math.max(1, amount)));
        sender.sendMessage("§a지급 완료: " + type.koName() + " 씨앗 x" + amount);
        return true;
    }
}
