/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package kr.wonguni.nationwar.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.plugin.java.JavaPlugin;

public class Database {
    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException classNotFoundException) {
            // empty catch block
        }
        try {
            File dbFile = new File(this.plugin.getDataFolder(), "nationwar.db");
            dbFile.getParentFile().mkdirs();
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            this.connection.setAutoCommit(true);
            this.createTables();
        }
        catch (SQLException e) {
            throw new RuntimeException("Failed to open SQLite", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = this.connection.createStatement();){
            st.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, balance INTEGER NOT NULL DEFAULT 0, combat_xp INTEGER NOT NULL DEFAULT 0, kills INTEGER NOT NULL DEFAULT 0, deaths INTEGER NOT NULL DEFAULT 0, assists INTEGER NOT NULL DEFAULT 0, jobs TEXT NOT NULL DEFAULT '')");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS nations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, leader_uuid TEXT NOT NULL, level INTEGER NOT NULL DEFAULT 1, treasury INTEGER NOT NULL DEFAULT 0, arrears_days INTEGER NOT NULL DEFAULT 0, raid_debt INTEGER NOT NULL DEFAULT 0, nexus_world TEXT, nexus_x INTEGER, nexus_y INTEGER, nexus_z INTEGER)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS nation_members (nation_id INTEGER NOT NULL, uuid TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'MEMBER', PRIMARY KEY (nation_id, uuid))");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_members_uuid ON nation_members(uuid)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS wars (id INTEGER PRIMARY KEY AUTOINCREMENT, nation_a INTEGER NOT NULL, nation_b INTEGER NOT NULL, state TEXT NOT NULL, created_at INTEGER NOT NULL)");
        }
    }

    public Connection connection() {
        return this.connection;
    }

    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
        }
    }
}

