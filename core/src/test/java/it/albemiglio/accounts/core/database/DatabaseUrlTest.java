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

    // The driver class must be named explicitly: a plugin's bundled driver is invisible to
    // DriverManager's classpath-scoped auto-registration, so HikariCP has to Class.forName it.
    @Test
    void sqliteNamesTheSqliteDriver() {
        assertEquals("org.sqlite.JDBC",
                new SQLite(null, 0, null, null, "x.db").driverClassName());
    }

    @Test
    void mysqlNamesTheMysqlDriver() {
        assertEquals("com.mysql.cj.jdbc.Driver",
                new MySQL("h", 3306, "u", "p", "d").driverClassName());
    }

    @Test
    void mariadbNamesTheMariadbDriver() {
        assertEquals("org.mariadb.jdbc.Driver",
                new MariaDB("h", 3306, "u", "p", "d").driverClassName());
    }
}
