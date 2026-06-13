package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

public class BungeeDatabase {
    public BungeeDatabase(BungeeKaosCore plugin, Configuration config) {
    }

    public Connection getConnection() throws SQLException {
        return null;
    }

    public void disconnect() {
    }

    public boolean connect(Configuration config) {
        return true;
    }
}
