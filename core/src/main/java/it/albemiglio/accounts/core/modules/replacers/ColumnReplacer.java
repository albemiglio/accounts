package it.albemiglio.accounts.core.modules.replacers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class ColumnReplacer extends Replacer {

    private final String table;
    private final String column;
    private final UuidCodec codec;

    public ColumnReplacer(String table, String column) {
        this(table, column, UuidCodec.DASHED);
    }

    public ColumnReplacer(String table, String column, UuidCodec codec) {
        this.table = table;
        this.column = column;
        this.codec = codec;
    }

    @Override
    public void replace(Connection connection, UUID oldId, UUID newId) throws SQLException {
        String sql = "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            codec.bind(ps, 1, newId);
            codec.bind(ps, 2, oldId);
            ps.executeUpdate();
        }
    }
}
