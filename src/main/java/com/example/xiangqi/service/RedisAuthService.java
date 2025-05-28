package com.example.xiangqi.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
public class RedisAuthService {
    RedisTemplate<String, Long> redisTemplate;

    // Save
    public void saveInvalidatedTokenExpirationKey(String invalidatedToken, Long timeExpiration) {
        redisTemplate.opsForValue().set(invalidatedToken, timeExpiration, timeExpiration, TimeUnit.MILLISECONDS);
    }

    // Get
    public Long getInvalidatedTokenExpirationKey(String invalidatedToken) {
        return redisTemplate.opsForValue().get(invalidatedToken);
    }
}
