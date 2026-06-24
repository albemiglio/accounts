package it.albemiglio.accounts.core.database;

import it.albemiglio.accounts.core.objects.enums.DBType;

public class SQLite extends DB {

    public SQLite(String host, int port, String username, String password, String database) {
        super(host, port, username, password, database);
        this.type = DBType.SQLITE;
    }

    @Override
    public String jdbcUrl() {
        return "jdbc:sqlite:" + getDatabase();
    }

    @Override
    public String driverClassName() {
        return "org.sqlite.JDBC";
    }

}
