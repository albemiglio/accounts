package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.modules.replacers.Replacer;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public abstract class Module {

    @Getter
    private final String name;
    private final Platform platform;
    private boolean running;
    @Getter
    private boolean enabled;
    private Optional<String> pluginName;

    private final DB database;
    private final Set<Replacer> replacers;

    public Module(String name, Platform platform) {
        this(name, platform, null);
    }

    public Module(String name, Platform platform, DB database) {
        this.name = name;
        this.platform = platform;
        this.database = database;
        this.running = false;
        this.enabled = false;
        this.pluginName = Optional.empty();
        this.replacers = new LinkedHashSet<>();
    }

    protected void addReplacer(Replacer replacer) {
        this.replacers.add(replacer);
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void reload() {}

    public void execute(Pair<UUID, UUID> migration) {
        try (Connection connection = database.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (Replacer replacer : replacers) {
                    replacer.replace(connection, migration.getLeft(), migration.getRight());
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new MigrationException("Migration failed for module " + name, e);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new MigrationException("Migration failed for module " + name, e);
        }
    }

    public void executeBatch(Collection<Pair<UUID, UUID>> tasks) {
        tasks.forEach(this::execute);
    }
}
