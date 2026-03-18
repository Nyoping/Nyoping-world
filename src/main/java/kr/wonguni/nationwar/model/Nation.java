/*
 * Decompiled with CFR 0.152.
 */
package kr.wonguni.nationwar.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Nation {
    private final String name;
    private UUID leaderUuid;
    private int level = 0;
    private long treasury = 0L;
    private final Deque<String> treasuryLogs = new ArrayDeque<String>();
    private int arrearsDays = 0;
    private String nexusWorld = null;
    private int nexusX;
    private int nexusY;
    private int nexusZ;
    private final Set<UUID> members = new HashSet<UUID>();
    private final Set<UUID> officers = new HashSet<UUID>();
    private int turretLevel = 0;
    private int inhibitorLevel = 0;
    private boolean relocationMode = false;
    private long lastRelocationAt = 0L;

    public Nation(String name) {
        this.name = name;
    }

    public Nation(String name, UUID leaderUuid) {
        this.name = name;
        this.leaderUuid = leaderUuid;
        if (leaderUuid != null) {
            this.members.add(leaderUuid);
        }
    }

    public String getName() {
        return this.name;
    }

    public UUID getLeaderUuid() {
        return this.leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    public long getTreasury() {
        return this.treasury;
    }

    public void setTreasury(long treasury) {
        this.treasury = Math.max(0L, treasury);
    }

    public List<String> getTreasuryLogs() {
        return new ArrayList<String>(this.treasuryLogs);
    }

    public void setTreasuryLogs(Collection<String> logs) {
        this.treasuryLogs.clear();
        if (logs == null) {
            return;
        }
        for (String s : logs) {
            if (s == null) continue;
            this.treasuryLogs.addLast(s);
        }
    }

    public void addTreasuryLog(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        this.treasuryLogs.addFirst(line);
        while (this.treasuryLogs.size() > 30) {
            this.treasuryLogs.removeLast();
        }
    }

    public int getArrearsDays() {
        return this.arrearsDays;
    }

    public void setArrearsDays(int arrearsDays) {
        this.arrearsDays = Math.max(0, arrearsDays);
    }

    public String getNexusWorld() {
        return this.nexusWorld;
    }

    public int getNexusX() {
        return this.nexusX;
    }

    public int getNexusY() {
        return this.nexusY;
    }

    public int getNexusZ() {
        return this.nexusZ;
    }

    public void setNexusWorld(String nexusWorld) {
        this.nexusWorld = nexusWorld;
    }

    public void setNexusX(int nexusX) {
        this.nexusX = nexusX;
    }

    public void setNexusY(int nexusY) {
        this.nexusY = nexusY;
    }

    public void setNexusZ(int nexusZ) {
        this.nexusZ = nexusZ;
    }

    public boolean hasNexus() {
        return this.nexusWorld != null;
    }

    public void setNexus(String world, int x, int y, int z) {
        this.nexusWorld = world;
        this.nexusX = x;
        this.nexusY = y;
        this.nexusZ = z;
    }

    public void clearNexus() {
        this.nexusWorld = null;
    }

    public Set<UUID> getMembers() {
        return this.members;
    }

    public Set<UUID> getOfficers() {
        return this.officers;
    }

    public void setOfficers(Collection<UUID> newOfficers) {
        this.officers.clear();
        if (newOfficers != null) {
            this.officers.addAll(newOfficers);
        }
        if (this.leaderUuid != null) {
            this.officers.add(this.leaderUuid);
        }
    }

    public boolean isOfficer(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return uuid.equals(this.leaderUuid) || this.officers.contains(uuid);
    }

    public void addOfficer(UUID uuid) {
        if (uuid != null) {
            this.officers.add(uuid);
        }
        if (this.leaderUuid != null) {
            this.officers.add(this.leaderUuid);
        }
    }

    public void removeOfficer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (uuid.equals(this.leaderUuid)) {
            return;
        }
        this.officers.remove(uuid);
    }

    public int getTurretLevel() {
        return this.turretLevel;
    }

    public void setTurretLevel(int turretLevel) {
        this.turretLevel = Math.max(0, turretLevel);
    }

    public int getInhibitorLevel() {
        return this.inhibitorLevel;
    }

    public void setInhibitorLevel(int inhibitorLevel) {
        this.inhibitorLevel = Math.max(0, inhibitorLevel);
    }

    public boolean isRelocationMode() {
        return this.relocationMode;
    }

    public void setRelocationMode(boolean relocationMode) {
        this.relocationMode = relocationMode;
    }

    public long getLastRelocationAt() {
        return this.lastRelocationAt;
    }

    public void setLastRelocationAt(long lastRelocationAt) {
        this.lastRelocationAt = Math.max(0L, lastRelocationAt);
    }

    public void setMembers(Collection<UUID> newMembers) {
        this.members.clear();
        if (newMembers != null) {
            this.members.addAll(newMembers);
        }
        if (this.leaderUuid != null) {
            this.members.add(this.leaderUuid);
        }
        if (this.leaderUuid != null) {
            this.officers.add(this.leaderUuid);
        }
    }

    public boolean isMember(UUID uuid) {
        return this.members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        this.members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        this.members.remove(uuid);
    }
}

