package com.example.xiangqi.service.my_sql;

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
    RedisTemplate<String, MatchContractEntity> mcRedisTemplate;
    RedisTemplate<String, Long> longRedisTemplate;

    // Key
    private static final String MATCH_CONTRACT_KEY = "matchContract:%s:";
    private static final String MATCH_CONTRACT_EXPIRATION_KEY = "matchContract:%s:expiration:";

    // Lock key
    private static final String MATCH_CONTRACT_LOCK_KEY = "lock:matchContract:%s:";

    // Time
    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final long RETRY_DELAY_MILLIS = 100;

    // Save
    public void saveMatchContract(String matchContractId, MatchContractEntity entity) {
        String key = String.format(MATCH_CONTRACT_KEY, matchContractId);
        mcRedisTemplate.opsForValue().set(key, entity);
    }

    public void saveMatchContractExpiration(String matchContractId, Long expirationTime) {
        String key = String.format(MATCH_CONTRACT_EXPIRATION_KEY, matchContractId);
        longRedisTemplate.opsForValue().set(key, expirationTime, expirationTime, TimeUnit.MILLISECONDS);
    }

    // Get
    public MatchContractEntity getMatchContract(String matchContractId) {
        return mcRedisTemplate.opsForValue().get(String.format(MATCH_CONTRACT_KEY, matchContractId));
    }

    // Delete
    public void deleteMatchContract(String matchContractId) {
        mcRedisTemplate.delete(String.format(MATCH_CONTRACT_KEY, matchContractId));
    }

    public void deleteMatchContractExpiration(String matchContractId) {
        mcRedisTemplate.delete(String.format(MATCH_CONTRACT_EXPIRATION_KEY, matchContractId));
    }

    // Acquire lock
    public void acquireMatchContractLock(String matchContractId) {
        String key = String.format(MATCH_CONTRACT_LOCK_KEY, matchContractId);
        while (true) {
            // Try to set the lock with a timeout
            Boolean success = mcRedisTemplate.opsForValue().setIfAbsent(key, new MatchContractEntity(), LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
        mcRedisTemplate.delete(key);
    }
}
