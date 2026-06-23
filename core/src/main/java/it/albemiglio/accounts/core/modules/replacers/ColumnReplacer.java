package it.albemiglio.accounts.core.modules.replacers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class ColumnReplacer extends Replacer {

    private final String table;
    private final String column;

    public ColumnReplacer(String table, String column) {
        this.table = table;
        this.column = column;
    }

    @Override
    public void replace(Connection connection, UUID oldId, UUID newId) throws SQLException {
        String sql = "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newId.toString());
            ps.setString(2, oldId.toString());
            ps.executeUpdate();
        }
    }
}
