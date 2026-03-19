package kr.wonguni.nationwar.service;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class HunterMobLevelListener implements Listener {
    private final NamespacedKey lvlKey;

    public HunterMobLevelListener(JavaPlugin plugin) {
        this.lvlKey = new NamespacedKey(plugin, "nw_mob_level");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;

        double x = Math.abs(le.getLocation().getX());
        double z = Math.abs(le.getLocation().getZ());
        double d = Math.max(x, z);
        int lvl = (int)(d / 100.0) + 1;
        if (lvl < 1) lvl = 1;
        if (lvl > 100) lvl = 100;

        le.getPersistentDataContainer().set(lvlKey, PersistentDataType.INTEGER, lvl);

        // 문서 §10.11: 100레벨 = 바닐라의 10배 → mult = 1 + (level-1)*(9/99)
        double mult = 1.0 + (lvl - 1) * (9.0 / 99.0);
        try {
            if (le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                double base = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1.0, base * mult));
                le.setHealth(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            }
            if (le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                double base = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
                le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(Math.max(0.0, base * mult));
            }
        } catch (Exception ignored) {}
    }
}
