package kr.wonguni.nationwar.core;

import java.io.File;
import java.io.IOException;
import java.util.*;
import kr.wonguni.nationwar.model.JobType;
import kr.wonguni.nationwar.model.Nation;
import kr.wonguni.nationwar.model.PlayerProfile;
import kr.wonguni.nationwar.model.StatType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DataStore {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerProfile> players = new HashMap<>();
    private final Map<String, Nation> nationsByNameLower = new HashMap<>();
    private final Map<UUID, String> playerNationNameLower = new HashMap<>();
    private final Set<String> placedOreKeys = new HashSet<>();
    private final Map<String, String> cropOwnerKeys = new HashMap<>();
    private final Map<String, String> cropTypeKeys = new HashMap<>();

    private File playersFile;
    private File nationsFile;
    private File cropOwnersFile;
    private File cropTypesFile;
    private File placedOresFile;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        nationsFile = new File(plugin.getDataFolder(), "nations.yml");
        cropOwnersFile = new File(plugin.getDataFolder(), "crop_owners.yml");
        cropTypesFile = new File(plugin.getDataFolder(), "crop_types.yml");
        placedOresFile = new File(plugin.getDataFolder(), "placed_ores.yml");
        ensure(playersFile);
        ensure(nationsFile);
        ensure(cropOwnersFile);
        ensure(cropTypesFile);
        ensure(placedOresFile);
        loadPlayers();
        loadNations();
        loadCropOwners();
        loadCropTypes();
        loadPlacedOres();
    }

    private void ensure(File f) {
        if (f.exists()) return;
        try { f.createNewFile(); } catch (IOException ignored) {}
    }

    public synchronized void saveAll() {
        savePlayers();
        saveNations();
        saveCropOwners();
        saveCropTypes();
        savePlacedOres();
    }

    public synchronized PlayerProfile getOrCreatePlayer(UUID uuid) {
        return players.computeIfAbsent(uuid, PlayerProfile::new);
    }

    private void loadPlayers() {
        players.clear();
        playerNationNameLower.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(playersFile);
        for (String key : yml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerProfile p = new PlayerProfile(uuid);
                p.setBalance(yml.getLong(key + ".balance", 0L));
                p.setStatPoints(yml.getInt(key + ".statPoints", 0));
                p.setStatXpRemainder(yml.getInt(key + ".statXpRem", 0));
                p.setCombatLevel(yml.getInt(key + ".combatLevel", 0));
                p.setKills(yml.getInt(key + ".k", 0));
                p.setDeaths(yml.getInt(key + ".d", 0));
                p.setAssists(yml.getInt(key + ".a", 0));
                p.setHungerReserve(yml.getInt(key + ".hungerReserve", 0));
                p.setShowOwnBorders(yml.getBoolean(key + ".showOwnBorders", false));
                p.setShowOtherBorders(yml.getBoolean(key + ".showOtherBorders", false));
                p.setPendingNexusOnLeave(yml.getBoolean(key + ".pendingNexusOnLeave", false));
                p.setLastFoodLevel(yml.getInt(key + ".lastFoodLevel", 20));
                p.setLastJobRemoveAt(yml.getLong(key + ".jobRemove.lastAt", 0L));

                LinkedHashSet<JobType> jobs = new LinkedHashSet<>();
                for (String js : yml.getStringList(key + ".jobs")) {
                    try { jobs.add(JobType.valueOf(js.trim().toUpperCase(Locale.ROOT))); } catch (Exception ignored) {}
                }
                p.setJobs(jobs);

                if (yml.getConfigurationSection(key + ".jobRanks") != null) {
                    for (String rk : yml.getConfigurationSection(key + ".jobRanks").getKeys(false)) {
                        try {
                            JobType jt = JobType.valueOf(rk.trim().toUpperCase(Locale.ROOT));
                            p.setJobRank(jt, yml.getInt(key + ".jobRanks." + rk, 0));
                        } catch (Exception ignored) {}
                    }
                }

                if (yml.getConfigurationSection(key + ".stats") != null) {
                    for (String sk : yml.getConfigurationSection(key + ".stats").getKeys(false)) {
                        StatType st = StatType.fromString(sk);
                        if (st != null) p.setStat(st, yml.getInt(key + ".stats." + sk, 0));
                    }
                }

                String nation = yml.getString(key + ".nation", null);
                if (nation != null && !nation.isBlank()) {
                    playerNationNameLower.put(uuid, nation.toLowerCase(Locale.ROOT));
                }
                players.put(uuid, p);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void savePlayers() {
        YamlConfiguration yml = new YamlConfiguration();
        for (PlayerProfile p : players.values()) {
            String key = p.getUuid().toString();
            yml.set(key + ".balance", p.getBalance());
            yml.set(key + ".statPoints", p.getStatPoints());
            yml.set(key + ".statXpRem", p.getStatXpRemainder());
            yml.set(key + ".combatLevel", p.getCombatLevel());
            yml.set(key + ".k", p.getKills());
            yml.set(key + ".d", p.getDeaths());
            yml.set(key + ".a", p.getAssists());
            yml.set(key + ".hungerReserve", p.getHungerReserve());
            yml.set(key + ".showOwnBorders", p.isShowOwnBorders());
            yml.set(key + ".showOtherBorders", p.isShowOtherBorders());
            yml.set(key + ".pendingNexusOnLeave", p.isPendingNexusOnLeave());
            yml.set(key + ".lastFoodLevel", p.getLastFoodLevel());
            yml.set(key + ".jobRemove.lastAt", p.getLastJobRemoveAt());

            List<String> jobs = new ArrayList<>();
            for (JobType jt : p.getJobs()) jobs.add(jt.name());
            yml.set(key + ".jobs", jobs);

            for (Map.Entry<JobType, Integer> e : p.getJobRanks().entrySet()) {
                yml.set(key + ".jobRanks." + e.getKey().name(), e.getValue());
            }
            for (Map.Entry<StatType, Integer> e : p.getStatsView().entrySet()) {
                yml.set(key + ".stats." + e.getKey().name(), e.getValue());
            }

            String lower = playerNationNameLower.get(p.getUuid());
            if (lower != null) {
                Nation n = nationsByNameLower.get(lower);
                yml.set(key + ".nation", n != null ? n.getName() : null);
            }
        }
        try { yml.save(playersFile); } catch (IOException ignored) {}
    }

    private void loadNations() {
        nationsByNameLower.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(nationsFile);
        for (String key : yml.getKeys(false)) {
            try {
                String name = yml.getString(key + ".name", key);
                Nation n = new Nation(name);
                String leader = yml.getString(key + ".leader", null);
                if (leader != null && !leader.isBlank()) {
                    try { n.setLeaderUuid(UUID.fromString(leader)); } catch (Exception ignored) {}
                }
                n.setLevel(yml.getInt(key + ".level", 0));
                n.setTreasury(yml.getLong(key + ".treasury", 0L));
                n.setTreasuryLogs(yml.getStringList(key + ".treasuryLogs"));
                n.setArrearsDays(yml.getInt(key + ".arrearsDays", 0));
                n.setTurretLevel(yml.getInt(key + ".structures.turretLevel", 0));
                n.setInhibitorLevel(yml.getInt(key + ".structures.inhibitorLevel", 0));
                n.setNexusWorld(yml.getString(key + ".nexus.world", null));
                n.setNexusX(yml.getInt(key + ".nexus.x", 0));
                n.setNexusY(yml.getInt(key + ".nexus.y", 0));
                n.setNexusZ(yml.getInt(key + ".nexus.z", 0));
                n.setRelocationMode(yml.getBoolean(key + ".nexus.relocationMode", false));
                n.setLastRelocationAt(yml.getLong(key + ".nexus.lastRelocationAt", 0L));

                Set<UUID> members = new HashSet<>();
                for (String ms : yml.getStringList(key + ".members")) {
                    try { members.add(UUID.fromString(ms)); } catch (Exception ignored) {}
                }
                n.setMembers(members);

                Set<UUID> officers = new HashSet<>();
                for (String os : yml.getStringList(key + ".officers")) {
                    try { officers.add(UUID.fromString(os)); } catch (Exception ignored) {}
                }
                n.setOfficers(officers);

                nationsByNameLower.put(n.getName().toLowerCase(Locale.ROOT), n);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveNations() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Nation n : nationsByNameLower.values()) {
            String key = n.getName();
            yml.set(key + ".name", n.getName());
            yml.set(key + ".leader", n.getLeaderUuid() == null ? null : n.getLeaderUuid().toString());
            yml.set(key + ".level", n.getLevel());
            yml.set(key + ".treasury", n.getTreasury());
            yml.set(key + ".treasuryLogs", n.getTreasuryLogs());
            yml.set(key + ".arrearsDays", n.getArrearsDays());
            yml.set(key + ".structures.turretLevel", n.getTurretLevel());
            yml.set(key + ".structures.inhibitorLevel", n.getInhibitorLevel());
            yml.set(key + ".nexus.world", n.getNexusWorld());
            yml.set(key + ".nexus.x", n.getNexusX());
            yml.set(key + ".nexus.y", n.getNexusY());
            yml.set(key + ".nexus.z", n.getNexusZ());
            yml.set(key + ".nexus.relocationMode", n.isRelocationMode());
            yml.set(key + ".nexus.lastRelocationAt", n.getLastRelocationAt());

            List<String> members = new ArrayList<>();
            for (UUID u : n.getMembers()) members.add(u.toString());
            yml.set(key + ".members", members);

            List<String> officers = new ArrayList<>();
            for (UUID u : n.getOfficers()) officers.add(u.toString());
            yml.set(key + ".officers", officers);
        }
        try { yml.save(nationsFile); } catch (IOException ignored) {}
    }

    public synchronized Nation getNationByName(String name) {
        if (name == null) return null;
        return nationsByNameLower.get(name.toLowerCase(Locale.ROOT));
    }

    public synchronized Nation getNationOf(UUID uuid) {
        String lower = playerNationNameLower.get(uuid);
        return lower == null ? null : nationsByNameLower.get(lower);
    }

    public synchronized void setNationOf(UUID uuid, Nation nation) {
        if (nation == null) playerNationNameLower.remove(uuid);
        else playerNationNameLower.put(uuid, nation.getName().toLowerCase(Locale.ROOT));
    }

    public synchronized Collection<Nation> getAllNations() {
        return Collections.unmodifiableCollection(nationsByNameLower.values());
    }

    public synchronized void putNation(Nation nation) {
        if (nation != null) nationsByNameLower.put(nation.getName().toLowerCase(Locale.ROOT), nation);
    }

    public synchronized void removeNation(String name) {
        if (name == null) return;
        nationsByNameLower.remove(name.toLowerCase(Locale.ROOT));
        playerNationNameLower.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(name));
    }

    public synchronized boolean isPlacedOre(String key) {
        return key != null && placedOreKeys.contains(key);
    }

    public synchronized void markPlacedOre(String key) {
        if (key != null) placedOreKeys.add(key);
    }

    public synchronized void unmarkPlacedOre(String key) {
        if (key != null) placedOreKeys.remove(key);
    }

    private void loadPlacedOres() {
        placedOreKeys.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(placedOresFile);
        placedOreKeys.addAll(yml.getStringList("ores"));
    }

    private void savePlacedOres() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("ores", new ArrayList<>(placedOreKeys));
        try { yml.save(placedOresFile); } catch (IOException ignored) {}
    }

    public synchronized void setCropOwner(String key, UUID owner) {
        if (key == null) return;
        if (owner == null) cropOwnerKeys.remove(key);
        else cropOwnerKeys.put(key, owner.toString());
    }

    public synchronized UUID getCropOwner(String key) {
        if (key == null) return null;
        String v = cropOwnerKeys.get(key);
        if (v == null) return null;
        try { return UUID.fromString(v); } catch (Exception e) { return null; }
    }

    public synchronized void clearCropOwner(String key) {
        if (key != null) cropOwnerKeys.remove(key);
    }

    private void loadCropOwners() {
        cropOwnerKeys.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(cropOwnersFile);
        if (yml.getConfigurationSection("owners") != null) {
            for (String k : yml.getConfigurationSection("owners").getKeys(false)) {
                String v = yml.getString("owners." + k, null);
                if (v != null) cropOwnerKeys.put(k, v);
            }
        }
    }

    private void saveCropOwners() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.createSection("owners", new HashMap<>(cropOwnerKeys));
        try { yml.save(cropOwnersFile); } catch (IOException ignored) {}
    }

    public synchronized void setCropType(String key, String type) {
        if (key == null) return;
        if (type == null) cropTypeKeys.remove(key);
        else cropTypeKeys.put(key, type);
    }

    public synchronized String getCropType(String key) {
        return key == null ? null : cropTypeKeys.get(key);
    }

    public synchronized void clearCropType(String key) {
        if (key != null) cropTypeKeys.remove(key);
    }

    private void loadCropTypes() {
        cropTypeKeys.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(cropTypesFile);
        if (yml.getConfigurationSection("types") != null) {
            for (String k : yml.getConfigurationSection("types").getKeys(false)) {
                String v = yml.getString("types." + k, null);
                if (v != null) cropTypeKeys.put(k, v);
            }
        }
    }

    private void saveCropTypes() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.createSection("types", new HashMap<>(cropTypeKeys));
        try { yml.save(cropTypesFile); } catch (IOException ignored) {}
    }
}
