package it.albemiglio.accounts.core.modules;

import java.util.Collection;

/**
 * The contribution point for community modules. A jar dropped in {@code plugins/accounts/jar-modules}
 * implements this and declares it in {@code META-INF/services/it.albemiglio.accounts.core.modules.ModuleProvider};
 * accounts discovers it via the {@link java.util.ServiceLoader} and adds whatever {@link Module}s it
 * returns. This is how someone adds support for a plugin accounts has never heard of — including custom
 * {@link Module} subclasses with their own storage logic (binary columns, exotic files, anything).
 */
public interface ModuleProvider {

    Collection<Module> modules();
}
