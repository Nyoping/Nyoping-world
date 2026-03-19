/*
 * Decompiled with CFR 0.152.
 */
package kr.wonguni.nationwar.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.StatType;

public class PlayerProfile {
    private final UUID uuid;
    private long balance;
    private int statPoints;
    private int statXpRemainder;
    private int combatLevel;
    private int kills;
    private int deaths;
    private int assists;
    private final Set<JobType> jobs = new LinkedHashSet<JobType>();
    private long lastJobRemoveAt;
    private final EnumMap<JobType, Integer> jobRanks = new EnumMap(JobType.class);
    private int hungerReserve;
    private int lastFoodLevel = 20;
    private boolean showOwnBorders;
    private boolean showOtherBorders;
    private boolean pendingNexusOnLeave;
    private final EnumMap<StatType, Integer> stats = new EnumMap(StatType.class);
    private final EnumMap<JobType, Integer> jobProficiency = new EnumMap(JobType.class);

    /** Proficiency level required to be eligible for each rank (index = rank 0..7). */
    public static final int[] RANK_PROMOTION_LEVELS = {0, 5, 10, 20, 30, 50, 70, 100};

    /** Proficiency XP required per level-up (flat 100 for all levels). */
    public static final int XP_PER_LEVEL = 100;

    /** Max proficiency level. */
    public static final int MAX_PROFICIENCY_LEVEL = 100;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.jobs.add(JobType.UNEMPLOYED);
        this.lastJobRemoveAt = 0L;
        for (StatType t : StatType.values()) {
            this.stats.put(t, 0);
        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public long getBalance() {
        return this.balance;
    }

    public void setBalance(long balance) {
        this.balance = Math.max(0L, balance);
    }

    public int getStatPoints() {
        return this.statPoints;
    }

    public void setStatPoints(int statPoints) {
        this.statPoints = Math.max(0, statPoints);
    }

    public int getStatXpRemainder() {
        return this.statXpRemainder;
    }

    public void setStatXpRemainder(int statXpRemainder) {
        this.statXpRemainder = Math.max(0, Math.min(99, statXpRemainder));
    }

    public int getCombatLevel() {
        return this.combatLevel;
    }

    public void setCombatLevel(int combatLevel) {
        this.combatLevel = Math.max(0, combatLevel);
    }

    public void addCombatLevel(int delta) {
        this.combatLevel = Math.max(0, this.combatLevel + delta);
    }

    public int getKills() {
        return this.kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public int getDeaths() {
        return this.deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public int getAssists() {
        return this.assists;
    }

    public void setAssists(int assists) {
        this.assists = Math.max(0, assists);
    }

    

public long getLastJobRemoveAt() {
    return this.lastJobRemoveAt;
}

public void setLastJobRemoveAt(long t) {
    this.lastJobRemoveAt = Math.max(0L, t);
}

public int getJobRank(JobType job) {
    if (job == null || job == JobType.UNEMPLOYED) {
        return 0;
    }
    Integer r = (Integer)this.jobRanks.get((Object)job);
    return r == null ? 0 : Math.max(0, Math.min(7, r));
}

public void setJobRank(JobType job, int rank) {
    if (job == null || job == JobType.UNEMPLOYED) {
        return;
    }
    int r = Math.max(0, Math.min(7, rank));
    this.jobRanks.put(job, r);
}

public Map<JobType, Integer> getJobRanks() {
    return this.jobRanks;
}

public Set<JobType> getJobs() {
        return this.jobs;
    }

    public void setJobs(Collection<JobType> newJobs) {
        this.jobs.clear();
        if (newJobs != null) {
            for (JobType j : newJobs) {
                if (j == null) continue;
                this.jobs.add(j);
            }
        }
        if (this.jobs.isEmpty()) {
            this.jobs.add(JobType.UNEMPLOYED);
        this.lastJobRemoveAt = 0L;
        }
        if (this.jobs.size() > 2) {
            Iterator<JobType> it = this.jobs.iterator();
            LinkedHashSet<JobType> keep = new LinkedHashSet<JobType>();
            while (it.hasNext() && keep.size() < 2) {
                keep.add(it.next());
            }
            this.jobs.clear();
            this.jobs.addAll(keep);
        }

// sync jobRanks with current jobs (UNEMPLOYED has no rank)
this.jobRanks.keySet().removeIf(j -> j == null || !this.jobs.contains(j) || j == JobType.UNEMPLOYED);
for (JobType j : this.jobs) {
    if (j == null || j == JobType.UNEMPLOYED) continue;
    this.jobRanks.putIfAbsent(j, 0);
}
    }

    public Map<StatType, Integer> getStatsView() {
        return Collections.unmodifiableMap(this.stats);
    }

    public int getStat(StatType type) {
        return this.stats.getOrDefault((Object)type, 0);
    }

    public void setStat(StatType type, int value) {
        if (type == null) {
            return;
        }
        this.stats.put(type, Math.max(0, value));
    }

    public void setStats(Map<StatType, Integer> newStats) {
        this.stats.clear();
        for (StatType t : StatType.values()) {
            this.stats.put(t, 0);
        }
        if (newStats != null) {
            for (Map.Entry entry : newStats.entrySet()) {
                if (entry.getKey() == null) continue;
                this.stats.put((StatType)((Object)entry.getKey()), Math.max(0, entry.getValue() == null ? 0 : (Integer)entry.getValue()));
            }
        }
    }

    public int getHungerReserve() {
        return this.hungerReserve;
    }

    public void setHungerReserve(int hungerReserve) {
        this.hungerReserve = Math.max(0, hungerReserve);
    }

    public int getLastFoodLevel() {
        return this.lastFoodLevel;
    }

    public void setLastFoodLevel(int lastFoodLevel) {
        this.lastFoodLevel = Math.max(0, Math.min(20, lastFoodLevel));
    }

    public boolean isShowOwnBorders() {
        return this.showOwnBorders;
    }

    public void setShowOwnBorders(boolean showOwnBorders) {
        this.showOwnBorders = showOwnBorders;
    }

    public boolean isShowOtherBorders() {
        return this.showOtherBorders;
    }

    public void setShowOtherBorders(boolean showOtherBorders) {
        this.showOtherBorders = showOtherBorders;
    }

    public boolean isPendingNexusOnLeave() {
        return this.pendingNexusOnLeave;
    }

    public void setPendingNexusOnLeave(boolean pendingNexusOnLeave) {
        this.pendingNexusOnLeave = pendingNexusOnLeave;
    }

    // --- Proficiency (숙련도) ---

    /** Get raw proficiency XP for a job. */
    public int getJobProficiency(JobType job) {
        if (job == null || job == JobType.UNEMPLOYED) return 0;
        Integer v = this.jobProficiency.get(job);
        return v == null ? 0 : v;
    }

    /** Get proficiency level (0~100) for a job. Level = totalXP / 100. */
    public int getJobProficiencyLevel(JobType job) {
        return Math.min(MAX_PROFICIENCY_LEVEL, getJobProficiency(job) / XP_PER_LEVEL);
    }

    /** Set raw proficiency XP for a job. */
    public void setJobProficiency(JobType job, int xp) {
        if (job == null || job == JobType.UNEMPLOYED) return;
        this.jobProficiency.put(job, Math.max(0, Math.min(MAX_PROFICIENCY_LEVEL * XP_PER_LEVEL, xp)));
    }

    /** Add proficiency XP for a job. Returns the new total. */
    public int addJobProficiency(JobType job, int xp) {
        if (job == null || job == JobType.UNEMPLOYED) return 0;
        int current = getJobProficiency(job);
        int max = MAX_PROFICIENCY_LEVEL * XP_PER_LEVEL;
        int newVal = Math.min(max, current + xp);
        this.jobProficiency.put(job, newVal);
        return newVal;
    }

    public Map<JobType, Integer> getJobProficiencyMap() {
        return this.jobProficiency;
    }

    /** Check if player meets proficiency level requirement for a given rank. */
    public boolean meetsRankProficiency(JobType job, int rank) {
        if (rank < 0 || rank >= RANK_PROMOTION_LEVELS.length) return false;
        return getJobProficiencyLevel(job) >= RANK_PROMOTION_LEVELS[rank];
    }
}

