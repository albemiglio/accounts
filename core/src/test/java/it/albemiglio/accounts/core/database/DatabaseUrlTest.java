package it.albemiglio.accounts.core.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseUrlTest {

    @Test
    void sqliteUrlPointsAtDatabaseFile() {
        assertEquals("jdbc:sqlite:/var/data/accounts.db",
                new SQLite(null, 0, null, null, "/var/data/accounts.db").jdbcUrl());
    }

    @Test
    void mysqlUrlHasHostPortAndDatabase() {
        assertEquals("jdbc:mysql://localhost:3306/accounts",
                new MySQL("localhost", 3306, "root", "secret", "accounts").jdbcUrl());
    }

    @Test
    void mariadbUrlHasHostPortAndDatabase() {
        assertEquals("jdbc:mariadb://db.local:3307/accounts",
                new MariaDB("db.local", 3307, "root", "secret", "accounts").jdbcUrl());
    }
}
