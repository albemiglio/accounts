package it.albemiglio.accounts.core.database;

import it.albemiglio.accounts.core.objects.enums.DBType;

public class MariaDB extends DB {

    public MariaDB(String host, int port, String username, String password, String database) {
        super(host, port, username, password, database);
        this.type = DBType.MARIADB;
    }

    @Override
    public String jdbcUrl() {
        return "jdbc:mariadb://" + getHost() + ":" + getPort() + "/" + getDatabase();
    }

    @Override
    public String driverClassName() {
        return "org.mariadb.jdbc.Driver";
    }
}
