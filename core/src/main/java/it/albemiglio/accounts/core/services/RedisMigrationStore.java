package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Redis-backed {@link MigrationStore}. Migrations live in a hash (id -> serialized Task) so they
 * survive restarts; the applied/failed sets hold {@code migrationId@instanceId} members. This is the
 * durability that lets an instance recover what it missed and makes partial migrations observable.
 */
public final class RedisMigrationStore implements MigrationStore {

    private static final String MIGRATIONS = "accounts:migrations";
    private static final String APPLIED = "accounts:applied";
    private static final String FAILED = "accounts:failed";

    private final JedisPool pool;

    public RedisMigrationStore(JedisPool pool) {
        this.pool = pool;
    }

    @Override
    public void record(Task task) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(MIGRATIONS, InstanceMigrator.migrationId(task), task.toString());
        }
    }

    @Override
    public Collection<Task> pending(String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> all = jedis.hgetAll(MIGRATIONS);
            List<Task> out = new ArrayList<>();
            for (Map.Entry<String, String> entry : all.entrySet()) {
                if (!jedis.sismember(APPLIED, member(entry.getKey(), instanceId))) {
                    out.add(Task.fromString(entry.getValue()));
                }
            }
            return out;
        }
    }

    @Override
    public boolean hasApplied(String migrationId, String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.sismember(APPLIED, member(migrationId, instanceId));
        }
    }

    @Override
    public void markApplied(String migrationId, String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(APPLIED, member(migrationId, instanceId));
        }
    }

    @Override
    public void markFailed(String migrationId, String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(FAILED, member(migrationId, instanceId));
        }
    }

    private static String member(String migrationId, String instanceId) {
        return migrationId + "@" + instanceId;
    }
}
