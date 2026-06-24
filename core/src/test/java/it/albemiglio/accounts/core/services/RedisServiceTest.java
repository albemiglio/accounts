package it.albemiglio.accounts.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisServiceTest {

    @Test
    void usesTheConfiguredEndpoint() {
        RedisService redis = new RedisService("redis.internal", 6390, "s3cret");

        assertEquals("redis.internal", redis.getHost());
        assertEquals(6390, redis.getPort());
    }

    @Test
    void defaultsToLocalhostWhenUnconfigured() {
        RedisService redis = new RedisService();

        assertEquals("localhost", redis.getHost());
        assertEquals(6379, redis.getPort());
    }
}
