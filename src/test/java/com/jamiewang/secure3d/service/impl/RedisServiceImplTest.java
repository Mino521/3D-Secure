package com.jamiewang.secure3d.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
public class RedisServiceImplTest {

    @Autowired
    private RedisServiceImpl redisService;

    @Test
    public void testWrite() {
        boolean result = redisService.writeOne("testKey", "testValue");
        assertTrue(result);
    }

    @Test
    public void testExist() {
        boolean exist = redisService.exists("testKey");
        assertTrue(exist);
    }

    @Test
    public void testFind() {
        Optional<String> res = redisService.findOne("testKey", String.class);
        assertTrue(res.isPresent());
    }

    @Test
    public void testDelete() {
        boolean rse = redisService.deleteOne("testKey");
        assertTrue(rse);
    }
}
