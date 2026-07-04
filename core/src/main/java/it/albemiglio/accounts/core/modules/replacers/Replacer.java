package it.albemiglio.accounts.core.modules.replacers;

import it.albemiglio.accounts.core.modules.Diagnosis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class Replacer {

    public abstract void replace(Connection connection, UUID oldId, UUID newId) throws SQLException;

    /**
     * Read-only pre-flight for {@code /accounts diagnose}: report whether a real player's uuid is present
     * where this replacer would rewrite it, and in the expected encoding. Default: nothing to report.
     */
    public List<Diagnosis> diagnose(Connection connection, UUID probe, String module) {
        return Collections.emptyList();
    }
}
