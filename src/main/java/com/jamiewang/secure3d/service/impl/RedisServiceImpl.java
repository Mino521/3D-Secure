package com.jamiewang.secure3d.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jamiewang.secure3d.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis service for basic CRUD operations
 *
 * Provides simple writeOne, deleteOne, and findOne operations for storing
 * and retrieving objects in Redis with JSON serialization.
 */
@Service
@Slf4j
public class RedisServiceImpl implements IRedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Write an object to Redis with custom TTL
     *
     * @param key Redis key
     * @param value Object to store (will be serialized to JSON)
     * @param <T> Type of object to store
     * @return true if successfully written, false otherwise
     */
    public <T> boolean writeOne(String key, T value) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Cannot write to Redis: key is null or empty");
            return false;
        }

        if (value == null) {
            log.warn("Cannot write to Redis: value is null for key: {}", key);
            return false;
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue);

            log.debug("Successfully wrote object to Redis with key: {}", key);
            return true;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON for key: {}", key, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to write object to Redis for key: {}", key, e);
            return false;
        }
    }

    /**
     * Find an object in Redis by key
     *
     * @param key Redis key
     * @param valueType Class type of the object to deserialize
     * @param <T> Type of object to retrieve
     * @return Optional containing the object if found, empty otherwise
     */
    public <T> Optional<T> findOne(String key, Class<T> valueType) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Cannot find in Redis: key is null or empty");
            return Optional.empty();
        }

        if (valueType == null) {
            log.warn("Cannot find in Redis: valueType is null for key: {}", key);
            return Optional.empty();
        }

        try {
            String jsonValue = redisTemplate.opsForValue().get(key);

            if (jsonValue == null) {
                log.debug("No value found in Redis for key: {}", key);
                return Optional.empty();
            }

            T object = objectMapper.readValue(jsonValue, valueType);
            log.debug("Successfully retrieved object from Redis for key: {}", key);

            return Optional.of(object);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON from Redis for key: {}", key, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve object from Redis for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Delete an object from Redis by key
     *
     * @param key Redis key to delete
     * @return true if the key was deleted, false if key didn't exist or deletion failed
     */
    public boolean deleteOne(String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Cannot delete from Redis: key is null or empty");
            return false;
        }

        try {
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Successfully deleted key from Redis: {}", key);
                return true;
            } else {
                log.debug("Key not found in Redis for deletion: {}", key);
                return false;
            }

        } catch (Exception e) {
            log.error("Failed to delete key from Redis: {}", key, e);
            return false;
        }
    }

    /**
     * Check if a key exists in Redis
     *
     * @param key Redis key to check
     * @return true if key exists, false otherwise
     */
    public boolean exists(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Failed to check key existence in Redis: {}", key, e);
            return false;
        }
    }

}
