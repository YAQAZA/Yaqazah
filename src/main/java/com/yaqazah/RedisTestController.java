package com.yaqazah;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
@RestController
@RequestMapping("/api/redis-test")
public class RedisTestController {


    private final StringRedisTemplate redisTemplate;

    // Spring automatically injects the Redis configuration from your application.yml/.env
    public RedisTestController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1. Test Writing to Upstash Redis
    @PostMapping("/set")
    public String setKey(@RequestParam String key, @RequestParam String value) {
        redisTemplate.opsForValue().set(key, value);
        return "Successfully saved to Redis! Key: " + key + " | Value: " + value;
    }

    // 2. Test Reading from Upstash Redis
    @GetMapping("/get")
    public String getKey(@RequestParam String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return "Key '" + key + "' not found in Redis.";
        }
        return "Retrieved from Redis -> " + key + ": " + value;
    }
    @GetMapping("/debug-host")
    public String debugHost() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory instanceof LettuceConnectionFactory) {
            LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
            return "Spring Boot is currently connected to Host: "
                    + lettuceFactory.getHostName()
                    + " on Port: " + lettuceFactory.getPort();
        }
        return "Using an unknown connection factory: " + factory.getClass().getName();
    }
}