package com.example.heritage_sharing_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private static final long CODE_EXPIRATION_SECONDS = 60;

    public String generateVerificationCode(String email) {
        String code = String.format("%04d", new Random().nextInt(10000));
        String key = VERIFICATION_CODE_PREFIX + email;

        try {
            logger.info("Attempting to connect to Redis server at 118.178.124.2:6379");
            redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
            logger.info("Successfully stored verification code in Redis for email: {}", email);
        } catch (Exception e) {
            logger.error("Failed to connect to Redis: {}", e.getMessage());
            logger.error("Redis connection error details:", e);
            throw new RuntimeException("Failed to connect to Redis: " + e.getMessage());
        }

        return code;
    }

    public boolean verifyCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        try {
            String storedCode = redisTemplate.opsForValue().get(key);
            return code != null && code.equals(storedCode);
        } catch (Exception e) {
            logger.error("Failed to connect to Redis: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to Redis: " + e.getMessage());
        }
    }

    public void removeCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Failed to connect to Redis: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to Redis: " + e.getMessage());
        }
    }
}
