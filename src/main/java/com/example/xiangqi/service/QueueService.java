package com.example.xiangqi.service;

import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class QueueService {
    RedisTemplate<String, String> redisTemplate;
    MatchService matchService;
    PlayerService playerService;

    private static final String QUEUE_KEY = "waitingPlayers:";
    private static final String LOCK_KEY = "lock:waitingPlayers:";

    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final long RETRY_DELAY_MILLIS = 100;

    public void queue() {
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long playerId = jwt.getClaim("uid");

        // Get current player's rank
        Integer playerRating = playerService.getRatingById(playerId);

        // Wait to acquire lock
        acquireLock();

        String opponentId = null;
        try {
            // Browse for opponent with equivalent rank
            Long listSize = redisTemplate.opsForList().size(QUEUE_KEY);

            for (int i = 0; i < listSize; i++) {
                String potentialOpponentId = redisTemplate.opsForList().index(QUEUE_KEY, i);
                if (potentialOpponentId != null && !potentialOpponentId.equals(String.valueOf(playerId))) {
                    Integer opponentRank = playerService.getRatingById(Long.valueOf(potentialOpponentId));
                    // Match if opponent's rank is equivalent
                    if (Math.abs(playerRating - opponentRank) <= 100) {
                        opponentId = potentialOpponentId;
                        // Remove opponent's ID from queue
                        redisTemplate.opsForList().remove(QUEUE_KEY, 1, opponentId);
                        break;
                    }
                }
            }
        } finally {
            // Always release the lock
            releaseLock();
        }

        if (opponentId != null) {
            // Match found! Create a new match
            matchService.createMatch(Long.valueOf(opponentId), playerId);
        } else {
            // Check if playerId already exists in the queue
            List<String> queue = redisTemplate.opsForList().range(QUEUE_KEY, 0, -1);
            if (queue != null && !queue.contains(String.valueOf(playerId))) {
                // No opponent yet, add this player to the queue
                redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(playerId));
            }
        }
    }

    public void unqueue(){
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long playerId = jwt.getClaim("uid");

        // Wait to acquire lock
        acquireLock();

        try {
            // Remove my Id from queue
            Long removeCount = redisTemplate.opsForList().remove(QUEUE_KEY, 1, String.valueOf(playerId));
            // Not found exception
            if (removeCount == 0)
                throw new AppException(ErrorCode.UNQUEUE_INVALID);
        } finally {
            // Release lock
            releaseLock();
        }
    }

    private void acquireLock() {
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

    private void releaseLock() {
        // Remove the lock
        redisTemplate.delete(LOCK_KEY);
    }
}
