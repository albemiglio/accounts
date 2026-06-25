package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis-backed {@link MigrationStore}. Migrations live in a hash (id -> serialized Task) so they
 * survive restarts; the applied/expected/failed sets are per-migration ({@code accounts:applied:<id>}
 * etc.) so the completion barrier can read who has applied vs who must.
 */
public final class RedisMigrationStore implements MigrationStore {

    private static final String MIGRATIONS = "accounts:migrations";
    private static final String APPLIED = "accounts:applied:";
    private static final String EXPECTED = "accounts:expected:";
    private static final String FAILED = "accounts:failed:";

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
                if (!jedis.sismember(APPLIED + entry.getKey(), instanceId)) {
                    out.add(Task.fromString(entry.getValue()));
                }
            }
            return out;
        }
    }

    @Override
    public boolean hasApplied(String migrationId, String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.sismember(APPLIED + migrationId, instanceId);
        }
    }

    @Override
    public void markApplied(String migrationId, String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(APPLIED + migrationId, instanceId);
        }
    }

    @Override
    public void markFailed(String migrationId, String instanceId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(FAILED + migrationId, instanceId);
        }
    }

    @Override
    public void recordExpected(String migrationId, Set<String> instances) {
        if (instances.isEmpty()) {
            return;
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(EXPECTED + migrationId, instances.toArray(new String[0]));
        }
    }

    @Override
    public Set<String> expectedInstances(String migrationId) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.smembers(EXPECTED + migrationId);
        }
    }

    @Override
    public Set<String> appliedInstances(String migrationId) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.smembers(APPLIED + migrationId);
        }
    }
}
