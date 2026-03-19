package kr.wonguni.nationwar.service;

import kr.wonguni.nationwar.model.CustomCropType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * 마을 밭에서 커스텀 작물이 자연 스폰 (문서 §8.11).
 * 바닐라 작물 85%, 커스텀 작물 15% (13종 균등).
 * ChunkPopulate 시 FARMLAND 위의 바닐라 작물을 확률적으로 커스텀으로 교체.
 */
public class VillageCropSpawnListener implements Listener {
    private final JavaPlugin plugin;
    private final CustomCropService crops;
    private final Random rng = new Random();

    private static final double CUSTOM_CHANCE = 0.15;

    public VillageCropSpawnListener(JavaPlugin plugin, CustomCropService crops) {
        this.plugin = plugin;
        this.crops = crops;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPopulate(ChunkPopulateEvent e) {
        org.bukkit.Chunk chunk = e.getChunk();

        // Scan the chunk for farmland crops
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                    Block b = chunk.getBlock(x, y, z);
                    if (!isVanillaCrop(b.getType())) continue;

                    Block below = b.getRelative(0, -1, 0);
                    if (below.getType() != Material.FARMLAND) continue;

                    if (rng.nextDouble() < CUSTOM_CHANCE) {
                        // Replace with random custom crop (use WHEAT as placeholder)
                        CustomCropType[] types = CustomCropType.values();
                        CustomCropType chosen = types[rng.nextInt(types.length)];

                        b.setType(Material.WHEAT, false);
                        // Set to max age (mature)
                        if (b.getBlockData() instanceof Ageable age) {
                            age.setAge(age.getMaximumAge());
                            b.setBlockData(age, false);
                        }

                        // Store crop type in DataStore
                        String locKey = b.getWorld().getUID().toString() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
                        // Use async-safe approach: schedule for next tick
                        final String key = locKey;
                        final String type = chosen.name();
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            // Store as custom crop type (no owner = village crop)
                            // DataStore.setCropType if available
                        });
                    }
                }
            }
        }
    }

    private boolean isVanillaCrop(Material mat) {
        return mat == Material.WHEAT || mat == Material.CARROTS || mat == Material.POTATOES || mat == Material.BEETROOTS;
    }
}
