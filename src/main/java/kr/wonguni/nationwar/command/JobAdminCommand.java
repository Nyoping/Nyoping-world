/*
 * NationWar - Job Admin Command
 * Admin-only tools for testing and operating job ranks and cooldown.
 */
package kr.wonguni.nationwar.command;

import java.util.Locale;
import kr.wonguni.nationwar.core.DataStore;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.service.JobQuestService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobAdminCommand implements CommandExecutor {
    private final DataStore store;
    private final JobQuestService quests;

    public JobAdminCommand(DataStore store, JobQuestService quests) {
        this.store = store;
        this.quests = quests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nationwar.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }
        if (args.length < 1) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("rank")) {
            // /jobadmin rank <player> <job> <0-7>
            if (args.length < 4) {
                sender.sendMessage("§c사용법: /jobadmin rank <player> <job> <0-7>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
                return true;
            }
            JobType job;
            try {
                job = JobType.valueOf(args[2].trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                sender.sendMessage("§c직업이 올바르지 않습니다.");
                return true;
            }
            int rank;
            try {
                rank = Integer.parseInt(args[3]);
            } catch (Exception e) {
                sender.sendMessage("§c등급 숫자가 올바르지 않습니다.");
                return true;
            }
            rank = Math.max(0, Math.min(7, rank));

            PlayerProfile prof = store.getOrCreatePlayer(target.getUniqueId());
            if (!prof.getJobs().contains(job)) {
                sender.sendMessage("§c해당 플레이어는 그 직업을 보유하고 있지 않습니다.");
                return true;
            }
            int current = prof.getJobRank(job);
            if (rank < current) {
                sender.sendMessage("§c등급 하락은 불가합니다. (필요하면 reset을 사용하세요)");
                return true;
            }
            prof.setJobRank(job, rank);
            store.savePlayers();
            if (target.isOnline()) {
                quests.syncJobRank(target, job, rank);
            }
            sender.sendMessage("§a설정 완료: " + target.getName() + " " + job.name() + " rank=" + rank);
            return true;
        }

        if (sub.equals("reset")) {
            // /jobadmin reset <player> <job|all>
            if (args.length < 3) {
                sender.sendMessage("§c사용법: /jobadmin reset <player> <job|all>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
                return true;
            }
            String jobArg = args[2].trim().toUpperCase(Locale.ROOT);
            PlayerProfile prof = store.getOrCreatePlayer(target.getUniqueId());

            if (jobArg.equals("ALL")) {
                for (JobType jt : prof.getJobs()) {
                    if (jt == null || jt == JobType.UNEMPLOYED) continue;
                    prof.getJobRanks().remove(jt);
                    if (target.isOnline()) quests.resetJob(target, jt);
                }
                prof.setJobs(java.util.List.of(JobType.UNEMPLOYED));
                prof.setLastJobRemoveAt(0L);
                store.savePlayers();
                if (target.isOnline()) quests.revokeRoot(target);
                sender.sendMessage("§a초기화 완료: " + target.getName() + " (ALL)");
                return true;
            }

            JobType job;
            try {
                job = JobType.valueOf(jobArg);
            } catch (Exception e) {
                sender.sendMessage("§c직업이 올바르지 않습니다. (또는 ALL)");
                return true;
            }
            prof.setJobRank(job, 0);
            store.savePlayers();
            if (target.isOnline()) {
                quests.resetJob(target, job);
                quests.syncJobRank(target, job, 0);
            }
            sender.sendMessage("§a초기화 완료: " + target.getName() + " " + job.name());
            return true;
        }

        if (sub.equals("cooldown")) {
            // /jobadmin cooldown clear <player>
            if (args.length < 3 || !args[1].equalsIgnoreCase("clear")) {
                sender.sendMessage("§c사용법: /jobadmin cooldown clear <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
                return true;
            }
            PlayerProfile prof = store.getOrCreatePlayer(target.getUniqueId());
            prof.setLastJobRemoveAt(0L);
            store.savePlayers();
            sender.sendMessage("§a제거 쿨타임 초기화 완료: " + target.getName());
            return true;
        }

        help(sender);
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§e/jobadmin rank <player> <job> <0-7>");
        sender.sendMessage("§e/jobadmin reset <player> <job|all>");
        sender.sendMessage("§e/jobadmin cooldown clear <player>");
    }
}
