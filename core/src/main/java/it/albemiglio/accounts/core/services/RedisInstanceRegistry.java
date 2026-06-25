package it.albemiglio.accounts.core.services;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis-backed {@link InstanceRegistry}: each instance heartbeats its id into a sorted set scored by
 * time; the active set is everyone whose heartbeat is recent. A permanently dead instance ages out,
 * so it cannot hold a migration's completion barrier open forever.
 */
public final class RedisInstanceRegistry implements InstanceRegistry {

    private static final String KEY = "accounts:instances";
    private static final long TTL_MILLIS = 30_000;

    private final JedisPool pool;
    private final String instanceId;

    public RedisInstanceRegistry(JedisPool pool, String instanceId) {
        this.pool = pool;
        this.instanceId = instanceId;
    }

    public void heartbeat() {
        try (Jedis jedis = pool.getResource()) {
            jedis.zadd(KEY, System.currentTimeMillis(), instanceId);
        }
    }

    @Override
    public Set<String> activeInstances() {
        try (Jedis jedis = pool.getResource()) {
            return new HashSet<>(jedis.zrangeByScore(KEY,
                    System.currentTimeMillis() - TTL_MILLIS, Double.POSITIVE_INFINITY));
        }
    }
}
