package com.example.xiangqi.service;

import com.example.xiangqi.entity.redis.MatchContractEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
public class RedisMatchContractService {
    RedisTemplate<String, MatchContractEntity> redisTemplate;

    // Key
    private static final String MATCH_CONTRACT_KEY = "matchContract:%s:";

    // Lock key
    private static final String MATCH_CONTRACT_LOCK_KEY = "lock:matchContract:%s:";

    // Time
    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final long RETRY_DELAY_MILLIS = 100;

    // Save
    public void saveMatchContract(String matchContractId, MatchContractEntity entity, Long timeExpiration) {
        String key = String.format(MATCH_CONTRACT_KEY, matchContractId);
        redisTemplate.opsForValue().set(key, entity, timeExpiration, TimeUnit.MILLISECONDS);
    }

    // Get
    public MatchContractEntity getMatchContract(String matchContractId) {
        return redisTemplate.opsForValue().get(String.format(MATCH_CONTRACT_KEY, matchContractId));
    }

    // Update
    public void updateMatchContract(String matchContractId, MatchContractEntity entity) {
        String key = String.format(MATCH_CONTRACT_KEY, matchContractId);

        // Get the remaining TTL in milliseconds
        long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        // Update the value and reapply the TTL
        redisTemplate.opsForValue().set(key, entity, ttl, TimeUnit.MILLISECONDS);
    }

    // Delete
    public void deleteMatchContract(String matchContractId) {
        redisTemplate.delete(String.format(MATCH_CONTRACT_KEY, matchContractId));
    }

    // Acquire lock
    public void acquireMatchContractLock(String matchContractId) {
        String key = String.format(MATCH_CONTRACT_LOCK_KEY, matchContractId);
        while (true) {
            // Try to set the lock with a timeout
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, new MatchContractEntity(), LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (success != null && success) {
                return;
            }
            try {
                Thread.sleep(RETRY_DELAY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting to retry lock acquisition", e);
            }
        }
    }

    // Release lock
    public void releaseMatchContractLock(String matchContractId) {
        String key = String.format(MATCH_CONTRACT_LOCK_KEY, matchContractId);
        // Remove the lock
        redisTemplate.delete(key);
    }
}
