package it.albemiglio.accounts.core.database;

import it.albemiglio.accounts.core.objects.enums.DBType;

public class MySQL extends DB {

    public MySQL(String host, int port, String username, String password, String database) {
        super(host, port, username, password, database);
        this.type = DBType.MYSQL;
    }

    @Override
    public String jdbcUrl() {
        return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDatabase();
    }

    @Override
    public String driverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }
}
