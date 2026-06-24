package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.Task;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collection;
import java.util.UUID;

/**
 * Wires the broadcast stack a platform plugin needs and is the one handle it keeps: build it on
 * enable, call {@link #migrate} from the command or Nyx, {@link #close} on disable. Starting it
 * subscribes for broadcasts and immediately recovers anything this instance missed while down.
 */
public final class AccountsEngine implements AutoCloseable {

    private final BroadcastMigrationService service;
    private final RedisMigrationSubscriber subscriber;
    private final JedisPool pool;

    private AccountsEngine(BroadcastMigrationService service, RedisMigrationSubscriber subscriber, JedisPool pool) {
        this.service = service;
        this.subscriber = subscriber;
        this.pool = pool;
    }

    public static AccountsEngine start(String host, int port, String password,
                                       String instanceId, Collection<Module> modules) {
        JedisPool pool = (password == null || password.isEmpty())
                ? new JedisPool(host, port)
                : new JedisPool(new JedisPoolConfig(), host, port, 2000, password);

        RedisMigrationStore store = new RedisMigrationStore(pool);
        InstanceMigrator migrator = new InstanceMigrator(instanceId, modules, store);
        RedisMigrationPublisher publisher = new RedisMigrationPublisher(pool);
        BroadcastMigrationService service = new BroadcastMigrationService(instanceId, migrator, store, publisher);

        RedisMigrationSubscriber subscriber = new RedisMigrationSubscriber(pool, service);
        subscriber.start();
        service.recoverPending();

        return new AccountsEngine(service, subscriber, pool);
    }

    public void migrate(Task task) {
        service.migrate(task);
    }

    /** Convenience entry for callers (the admin command, Nyx) that have the raw ids. */
    public void migrate(UUID from, UUID to, String username) {
        Task task = new Task();
        task.setMigration(Pair.of(from, to));
        task.setUsername(username == null ? "" : username);
        task.setCurrFailures(0);
        service.migrate(task);
    }

    @Override
    public void close() {
        subscriber.stop();
        pool.close();
    }
}
