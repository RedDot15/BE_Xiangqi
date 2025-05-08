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
    public class RedisGameService {
        private final RedisTemplate<String, Object> redisTemplate;

    private static final String BOARD_KEY_PREFIX = "match:%d:board:";
    private static final String PLAYER_KEY_PREFIX = "match:%d:%sPlayer:id:";
    private static final String TURN_KEY_PREFIX = "match:%d:turn:";
    private static final String PLAYER_TOTAL_TIME_KEY_PREFIX = "match:%d:%sPlayer:totalTimeLeft:";
    private static final String PLAYER_NAME_KEY_PREFIX = "match:%d:%sPlayer:name:";
    private static final String PLAYER_RATING_KEY_PREFIX = "match:%d:%sPlayer:rating:";
    private static final String LAST_MOVE_TIME_KEY_PREFIX = "match:%d:lastMoveTime:";
    private static final String PLAYER_READY_STATUS_KEY_PREFIX = "match:%d:%sPlayer:readyStatus:";
    private static final String PLAYER_TURN_TIME_EXPIRATION_KEY = "match:%d:turnTimeExpiration:";
    private static final String PLAYER_TOTAL_TIME_EXPIRATION_KEY = "match:%d:totalTimeExpiration:";
    private static final String MATCH_INITIAL_LOCK_KEY = "lock:matchInitial:match:%d";

    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final long RETRY_DELAY_MILLIS = 100;

    // Save
    public void saveBoardStateJson(Long matchId, String boardStateJson) {
        redisTemplate.opsForValue().set(String.format(BOARD_KEY_PREFIX, matchId), boardStateJson);
    }

    public void savePlayerId(Long matchId, Long playerId, boolean isRedPlayer) {
        redisTemplate.opsForValue().set(String.format(PLAYER_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"), playerId);
    }

    public void saveTurn(Long matchId, Long turn) {
        redisTemplate.opsForValue().set(String.format(TURN_KEY_PREFIX, matchId), turn);
    }

    public void savePlayerName(Long matchId, String name, boolean isRedPlayer) {
        redisTemplate.opsForValue().set(String.format(PLAYER_NAME_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"), name);
    }

    public void savePlayerRating(Long matchId, Integer rating, boolean isRedPlayer) {
        redisTemplate.opsForValue().set(String.format(PLAYER_RATING_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"), rating);
    }

    public void savePlayerTotalTimeLeft(Long matchId, Long timeLeft, boolean isRedPlayer) {
        redisTemplate.opsForValue().set(String.format(PLAYER_TOTAL_TIME_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"), timeLeft);
    }

    public void saveLastMoveTime(Long matchId, Instant time) {
        redisTemplate.opsForValue().set(String.format(LAST_MOVE_TIME_KEY_PREFIX, matchId), String.valueOf(time.toEpochMilli()));
    }

    public void savePlayerReadyStatus(Long matchId, boolean isRedPlayer, boolean readyStatus) {
        redisTemplate.opsForValue().set(String.format(PLAYER_READY_STATUS_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"), readyStatus);
    }

    public void savePlayerTurnTimeExpiration(Long matchId, Long timeExpiration) {
        String key = String.format(PLAYER_TURN_TIME_EXPIRATION_KEY, matchId);
        redisTemplate.opsForValue().set(key, timeExpiration, timeExpiration, TimeUnit.MILLISECONDS);
    }

    public void savePlayerTotalTimeExpiration(Long matchId, Long timeExpiration) {
        String key = String.format(PLAYER_TOTAL_TIME_EXPIRATION_KEY, matchId);
        redisTemplate.opsForValue().set(key, timeExpiration, timeExpiration, TimeUnit.MILLISECONDS);
    }

    public void saveAiMode(Long matchId, String aiMode) {
        redisTemplate.opsForValue().set(String.format("match:%d:aiMode", matchId), aiMode);
    }


    public void acquireMatchInitialLock(Long matchId) {
        while (true) {
            // Try to set the lock with a timeout
            Boolean success = redisTemplate.opsForValue().setIfAbsent(String.format(MATCH_INITIAL_LOCK_KEY, matchId), "locked", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

    // Get
    public String getBoardStateJson(Long matchId) {
        return (String) Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(BOARD_KEY_PREFIX, matchId)));
    }

    public String getAiMode(Long matchId) {
        return (String) redisTemplate.opsForValue().get(String.format("match:%d:aiMode", matchId));
    }

    public Long getPlayerId(Long matchId, boolean isRedPlayer) {
        return ((Number) (Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(PLAYER_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"))))).longValue();
    }

    public Long getTurn(Long matchId) {
        return ((Number) (Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(TURN_KEY_PREFIX, matchId))))).longValue();
    }

    public String getPlayerName(Long matchId, boolean isRedPlayer) {
        return (String) Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(PLAYER_NAME_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black")));
    }

    public Integer getPlayerRating(Long matchId, boolean isRedPlayer) {
        return (Integer) Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(PLAYER_RATING_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black")));
    }

    public Long getPlayerTotalTimeLeft(Long matchId, boolean isRedPlayer) {
        return ((Number) Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(PLAYER_TOTAL_TIME_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black")))).longValue();
    }

    public Instant getLastMoveTime(Long matchId) {
        String value = (String) redisTemplate.opsForValue().get(String.format(LAST_MOVE_TIME_KEY_PREFIX, matchId));
        return value != null ? Instant.ofEpochMilli(Long.parseLong(value)) : null;
    }

    public Boolean getPlayerReadyStatus(Long matchId, boolean isRedPlayer) {
        return (Boolean) Objects.requireNonNull(redisTemplate.opsForValue().get(String.format(PLAYER_READY_STATUS_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black")));
    }

    // Delete
    public void deleteBoardState(Long matchId) {
        redisTemplate.delete(String.format(BOARD_KEY_PREFIX, matchId));
    }

    public void deletePlayerId(Long matchId, boolean isRedPlayer) {
        redisTemplate.delete(String.format(PLAYER_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"));
    }

    public void deleteTurn(Long matchId) {
        redisTemplate.delete(String.format(TURN_KEY_PREFIX, matchId));
    }

    public void deletePlayerName(Long matchId, boolean isRedPlayer) {
        redisTemplate.delete(String.format(PLAYER_NAME_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"));
    }

    public void deletePlayerRating(Long matchId, boolean isRedPlayer) {
        redisTemplate.delete(String.format(PLAYER_RATING_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"));
    }

    public void deleteTotalPlayerTimeLeft(Long matchId, boolean isRedPlayer) {
        redisTemplate.delete(String.format(PLAYER_TOTAL_TIME_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"));
    }

    public void deleteLastMoveTime(Long matchId) {
        redisTemplate.delete(String.format(LAST_MOVE_TIME_KEY_PREFIX, matchId));
    }

    public void deletePlayerReadyStatus(Long matchId, boolean isRedPlayer) {
        redisTemplate.delete(String.format(PLAYER_READY_STATUS_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"));
    }

    public void deletePlayerTurnTimeExpiration(Long matchId) {
        redisTemplate.delete(String.format(PLAYER_TURN_TIME_EXPIRATION_KEY, matchId));
    }

    public void deletePlayerTotalTimeExpiration(Long matchId) {
        redisTemplate.delete(String.format(PLAYER_TOTAL_TIME_EXPIRATION_KEY, matchId));
    }

    public void releaseMatchInitialLock(Long matchId) {
        redisTemplate.delete(String.format(MATCH_INITIAL_LOCK_KEY, matchId));
    }

}
