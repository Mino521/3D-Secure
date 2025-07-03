package com.jamiewang.secure3d.service;

import java.util.Optional;

public interface IRedisService {

    /**
     * Write an object to Redis with custom TTL
     *
     * @param key Redis key
     * @param value Object to store (will be serialized to JSON)
     * @param <T> Type of object to store
     * @return true if successfully written, false otherwise
     */
    public <T> boolean writeOne(String key, T value);

    /**
     * Find an object in Redis by key
     *
     * @param key Redis key
     * @param valueType Class type of the object to deserialize
     * @param <T> Type of object to retrieve
     * @return Optional containing the object if found, empty otherwise
     */
    public <T> Optional<T> findOne(String key, Class<T> valueType);

    /**
     * Delete an object from Redis by key
     *
     * @param key Redis key to delete
     * @return true if the key was deleted, false if key didn't exist or deletion failed
     */
    public boolean deleteOne(String key);

    /**
     * Check if a key exists in Redis
     *
     * @param key Redis key to check
     * @return true if key exists, false otherwise
     */
    public boolean exists(String key);

}
