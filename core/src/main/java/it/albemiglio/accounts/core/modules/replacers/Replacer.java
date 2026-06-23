package it.albemiglio.accounts.core.modules.replacers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public abstract class Replacer {

    public abstract void replace(Connection connection, UUID oldId, UUID newId) throws SQLException;
}
