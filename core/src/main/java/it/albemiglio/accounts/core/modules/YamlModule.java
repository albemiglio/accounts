package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.modules.replacers.Replacer;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.util.Collection;

public class YamlModule extends Module {

    public YamlModule(String name, Platform platform, DB database, Collection<Replacer> replacers) {
        this(name, platform, database, replacers, false);
    }

    public YamlModule(String name, Platform platform, DB database, Collection<Replacer> replacers,
                      boolean disableForeignKeyChecks) {
        super(name, platform, database);
        replacers.forEach(this::addReplacer);
        if (disableForeignKeyChecks) {
            disableForeignKeyChecks();
        }
    }
}
