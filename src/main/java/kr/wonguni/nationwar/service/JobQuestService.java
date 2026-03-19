package kr.wonguni.nationwar.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Locale;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Job quest skeleton via Advancements (datapack generated per-world).
 * - Disables vanilla advancements through datapack filtering
 * - Creates a custom advancement tab: nationwarjobs:root
 * - Creates placeholder advancements for each job rank: nationwarjobs:job/<job>/<rank>
 *
 * NOTE: To apply datapack, use /reload or server restart.
 */
public class JobQuestService {
    private final JavaPlugin plugin;

    public JobQuestService(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public void ensureDatapack() {
        Bukkit.getWorlds().forEach(w -> {
            try {
                File dpDir = new File(w.getWorldFolder(), "datapacks/NationWarJobs");
                File advDir = new File(dpDir, "data/nationwarjobs/advancements");
                if (!advDir.exists() && !advDir.mkdirs()) return;

                writePackMeta(dpDir);
                writeRoot(advDir);
                writeJobAdvancements(advDir);
            } catch (Exception ex) {
                plugin.getLogger().warning("[NationWar] Failed to generate job datapack for world " + w.getName() + ": " + ex.getMessage());
            }
        });

        plugin.getLogger().info("[NationWar] JobQuest datapack ensured. (Apply with /reload or server restart)");
    }

    private void writePackMeta(File dpDir) throws IOException {
        File meta = new File(dpDir, "pack.mcmeta");
        String json = "{\n" +
                "  \"pack\": {\n" +
                "    \"description\": \"NationWar - Job Quests\",\n" +
                "    \"min_format\": [94, 1],\n" +
                "    \"max_format\": [94, 1]\n" +
                "  },\n" +
                "  \"filter\": {\n" +
                "    \"block\": [\n" +
                "      {\n" +
                "        \"namespace\": \"minecraft\",\n" +
                "        \"path\": \"advancements/.*\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n";
        write(meta, json);
    }

    private void writeRoot(File advDir) throws IOException {
        File root = new File(advDir, "root.json");
        String json = "{\n" +
                "  \"display\": {\n" +
                "    \"icon\": { \"item\": \"minecraft:book\" },\n" +
                "    \"title\": { \"text\": \"직업 발전과제\" },\n" +
                "    \"description\": { \"text\": \"NationWar 직업 탭\" },\n" +
                "    \"background\": \"minecraft:textures/gui/advancements/backgrounds/stone.png\",\n" +
                "    \"show_toast\": false,\n" +
                "    \"announce_to_chat\": false,\n" +
                "    \"hidden\": false\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"impossible\": { \"trigger\": \"minecraft:impossible\" }\n" +
                "  }\n" +
                "}\n";
        write(root, json);
    }

    private void writeJobAdvancements(File advDir) throws IOException {
        int row = 0;
        for (JobType job : JobType.values()) {
            if (job == JobType.UNEMPLOYED) continue;
            File jobDir = new File(advDir, "job/" + job.name().toLowerCase(Locale.ROOT));
            if (!jobDir.exists()) jobDir.mkdirs();

            for (int rank = 0; rank <= 7; rank++) {
                File f = new File(jobDir, rank + ".json");

                String parent;
                int x;
                int y;
                if (rank == 0) {
                    parent = "nationwarjobs:root";
                    x = 2;
                    y = row * 2;
                } else {
                    parent = "nationwarjobs:job/" + job.name().toLowerCase(Locale.ROOT) + "/" + (rank - 1);
                    x = 2 + rank;
                    y = row * 2;
                }

                String title = koreanJob(job) + " " + (rank + 1) + "단계";
                String description = "세부 내용은 추후 추가 예정";
                String json = "{\n" +
                        "  \"parent\": \"" + parent + "\",\n" +
                        "  \"display\": {\n" +
                        "    \"icon\": { \"item\": \"" + iconFor(job) + "\" },\n" +
                        "    \"title\": { \"text\": \"" + escape(title) + "\" },\n" +
                        "    \"description\": { \"text\": \"" + escape(description) + "\" },\n" +
                        "    \"frame\": \"task\",\n" +
                        "    \"show_toast\": false,\n" +
                        "    \"announce_to_chat\": false,\n" +
                        "    \"hidden\": false,\n" +
                        "    \"x\": " + x + ",\n" +
                        "    \"y\": " + y + "\n" +
                        "  },\n" +
                        "  \"criteria\": {\n" +
                        "    \"impossible\": { \"trigger\": \"minecraft:impossible\" }\n" +
                        "  }\n" +
                        "}\n";
                write(f, json);
            }
            row++;
        }
    }

    public void syncPlayer(Player p, PlayerProfile prof) {
        if (p == null) return;
        revokeVanillaAdvancements(p);

        for (JobType job : JobType.values()) {
            if (job == null || job == JobType.UNEMPLOYED) continue;
            resetJob(p, job);
        }
        revokeRoot(p);

        if (prof == null || prof.getJobs() == null) return;

        boolean hasAny = false;
        for (JobType job : prof.getJobs()) {
            if (job == null || job == JobType.UNEMPLOYED) continue;
            hasAny = true;
            syncJobRank(p, job, prof.getJobRank(job));
        }
        if (!hasAny) {
            revokeRoot(p);
        }
    }

    public void revokeVanillaAdvancements(Player p) {
        if (p == null) return;
        Iterator<Advancement> it = Bukkit.advancementIterator();
        while (it.hasNext()) {
            Advancement adv = it.next();
            if (adv == null) continue;
            NamespacedKey key = adv.getKey();
            if (key == null || !"minecraft".equalsIgnoreCase(key.getNamespace())) continue;
            AdvancementProgress prog = p.getAdvancementProgress(adv);
            for (String c : prog.getAwardedCriteria()) {
                prog.revokeCriteria(c);
            }
        }
    }

    public void grantJobRank(Player p, JobType job, int rank) {
        if (job == null || job == JobType.UNEMPLOYED) return;
        rank = Math.max(0, Math.min(7, rank));
        NamespacedKey key = new NamespacedKey("nationwarjobs", "job/" + job.name().toLowerCase(Locale.ROOT) + "/" + rank);
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return;
        AdvancementProgress prog = p.getAdvancementProgress(adv);
        if (!prog.isDone()) {
            prog.awardCriteria("impossible");
        }
    }

    public void revokeJobRank(Player p, JobType job, int rank) {
        if (job == null || job == JobType.UNEMPLOYED) return;
        rank = Math.max(0, Math.min(7, rank));
        NamespacedKey key = new NamespacedKey("nationwarjobs", "job/" + job.name().toLowerCase(Locale.ROOT) + "/" + rank);
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return;
        AdvancementProgress prog = p.getAdvancementProgress(adv);
        for (String c : prog.getAwardedCriteria()) {
            prog.revokeCriteria(c);
        }
    }

    public void grantRoot(Player p) {
        NamespacedKey key = new NamespacedKey("nationwarjobs", "root");
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return;
        AdvancementProgress prog = p.getAdvancementProgress(adv);
        if (!prog.isDone()) {
            prog.awardCriteria("impossible");
        }
    }

    public void revokeRoot(Player p) {
        NamespacedKey key = new NamespacedKey("nationwarjobs", "root");
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return;
        AdvancementProgress prog = p.getAdvancementProgress(adv);
        for (String c : prog.getAwardedCriteria()) {
            prog.revokeCriteria(c);
        }
    }

    public void syncJobRank(Player p, JobType job, int rank) {
        if (p == null || job == null || job == JobType.UNEMPLOYED) return;
        rank = Math.max(0, Math.min(7, rank));
        this.grantRoot(p);
        for (int r = 0; r <= rank; r++) {
            this.grantJobRank(p, job, r);
        }
        for (int r = rank + 1; r <= 7; r++) {
            this.revokeJobRank(p, job, r);
        }
    }

    public void resetJob(Player p, JobType job) {
        if (p == null || job == null || job == JobType.UNEMPLOYED) return;
        for (int r = 0; r <= 7; r++) {
            this.revokeJobRank(p, job, r);
        }
    }

    private String iconFor(JobType job) {
        return switch (job) {
            case MINER -> "minecraft:iron_pickaxe";
            case FARMER -> "minecraft:wheat";
            case COOK -> "minecraft:cooked_beef";
            case FISHER -> "minecraft:fishing_rod";
            case HUNTER -> "minecraft:bow";
            case BREWER -> "minecraft:brewing_stand";
            default -> "minecraft:paper";
        };
    }

    private String koreanJob(JobType job) {
        return JobService.koreanName(job);
    }

    private void write(File file, String content) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
