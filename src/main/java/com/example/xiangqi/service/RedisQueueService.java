package com.example.xiangqi.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
    public class RedisQueueService {
        private final RedisTemplate<String, Object> redisTemplate;

    private static final String PLAYER_ACCEPT_STATUS_KEY_PREFIX = "queue:playerId:%d:acceptStatus:";
    private static final String MATCH_ACCEPT_EXPIRATION_KEY = "player1Id:%d:player2Id:%d:matchAcceptExpiration:";

    private static final String LOCK_KEY = "lock:waitingPlayers:";
    private static final String MATCH_ACCEPT_LOCK_KEY = "lock:matchAccept:player1Id:%d:player2Id:%d:";

    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final long RETRY_DELAY_MILLIS = 100;

    // Save
    public void savePlayerAcceptStatus(Long playerId, boolean acceptStatus) {
        redisTemplate.opsForValue().set(String.format(PLAYER_ACCEPT_STATUS_KEY_PREFIX, playerId), acceptStatus);
    }

    public void saveMatchAcceptExpiration(Long player1Id, Long player2Id, Long timeExpiration) {
        String key = String.format(MATCH_ACCEPT_EXPIRATION_KEY, Math.min(player1Id,player2Id), Math.max(player1Id, player2Id));
        redisTemplate.opsForValue().set(key, timeExpiration, timeExpiration, TimeUnit.MILLISECONDS);
    }

    // Get
    public Boolean getPlayerAcceptStatus(Long playerId) {
        return (Boolean) Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(PLAYER_ACCEPT_STATUS_KEY_PREFIX, playerId)));
    }

    public Long getMatchAcceptExpiration(Long player1Id, Long player2Id) {
        String key = String.format(MATCH_ACCEPT_EXPIRATION_KEY, Math.min(player1Id,player2Id), Math.max(player1Id, player2Id));
        Number number = (Number) redisTemplate.opsForValue().get(key);
        return number == null ? null : number.longValue();
    }

    // Delete
    public void deletePlayerAcceptStatus(Long playerId) {
        redisTemplate.delete(String.format(PLAYER_ACCEPT_STATUS_KEY_PREFIX, playerId));
    }

    public void deleteMatchAcceptExpiration(Long player1Id, Long player2Id) {
        redisTemplate.delete(String.format(MATCH_ACCEPT_EXPIRATION_KEY, player1Id, player2Id));
    }

    // Acquire lock
    public void acquireLock() {
        while (true) {
            // Try to set the lock with a timeout
            Boolean success = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

    public void acquireMatchAcceptLock(Long player1Id, Long player2Id) {
        while (true) {
            // Try to set the lock with a timeout
            Boolean success = redisTemplate.opsForValue().setIfAbsent(String.format(MATCH_ACCEPT_LOCK_KEY, Math.min(player1Id, player2Id), Math.max(player1Id, player2Id)), "locked", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
    public void releaseLock() {
        // Remove the lock
        redisTemplate.delete(LOCK_KEY);
    }

    public void releaseMatchAcceptLock(Long player1Id, Long player2Id) {
        // Remove the lock
        redisTemplate.delete(String.format(MATCH_ACCEPT_LOCK_KEY, Math.min(player1Id, player2Id), Math.max(player1Id, player2Id)));
    }
}
