package it.albemiglio.accounts.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.albemiglio.accounts.core.objects.enums.DBType;
import lombok.AccessLevel;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;

@Getter
public abstract class DB {

    // TODO: aggiungi tutti i dati generici per collegarsi a un qualsiasi tipo di database

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String database;

    protected DBType type;

    @Getter(AccessLevel.NONE)
    private HikariDataSource dataSource;

    public DB(String host, int port, String username, String password, String database) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public abstract String jdbcUrl();

    public synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl());
            if (username != null) {
                config.setUsername(username);
            }
            if (password != null) {
                config.setPassword(password);
            }
            dataSource = new HikariDataSource(config);
        }
        return dataSource.getConnection();
    }

    public synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
