package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * Subscribes to the broadcast channel on a dedicated connection and hands every received migration to
 * the {@link BroadcastMigrationService}. The subscribe call blocks its connection for life, which is
 * why it runs on its own thread with its own pooled resource.
 */
public final class RedisMigrationSubscriber {

    private final JedisPool pool;
    private final JedisPubSub pubSub;
    private Thread thread;

    public RedisMigrationSubscriber(JedisPool pool, BroadcastMigrationService service) {
        this.pool = pool;
        this.pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                service.handle(Task.fromString(message));
            }
        };
    }

    public void start() {
        thread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(pubSub, RedisMigrationPublisher.CHANNEL);
            } catch (Exception ignored) {
                // connection dropped; the plugin lifecycle restarts the subscriber
            }
        }, "accounts-migration-subscriber");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
        if (thread != null) {
            thread.interrupt();
        }
    }
}
