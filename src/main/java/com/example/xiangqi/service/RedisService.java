package com.example.xiangqi.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
    public class RedisService {
        private final RedisTemplate<String, Object> redisTemplate;

    private static final String BOARD_KEY_PREFIX = "match:%d:board:";
    private static final String PLAYER_KEY_PREFIX = "match:%d:%sPlayer:id:";
    private static final String TURN_KEY_PREFIX = "match:%d:turn:";

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

    // Get
    public String getBoardStateJson(Long matchId) {
        return (String) redisTemplate.opsForValue().get(String.format(BOARD_KEY_PREFIX, matchId));
    }

    public Long getPlayerId(Long matchId, boolean isRedPlayer) {
        return ((Number) redisTemplate.opsForValue().get(String.format(PLAYER_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"))).longValue();
    }

    public Long getTurn(Long matchId) {
        return ((Number) redisTemplate.opsForValue().get(String.format(TURN_KEY_PREFIX, matchId))).longValue();
    }

    // Delete
    public void deleteBoardState(Long matchId) {
        redisTemplate.delete(String.format(BOARD_KEY_PREFIX, matchId));
    }

    public void deletePlayerId(Long matchId, boolean isRedPlayer) {
        redisTemplate.delete(String.format(BOARD_KEY_PREFIX, matchId, isRedPlayer ? "red" : "black"));
    }

    public void deleteTurn(Long matchId) {
        redisTemplate.delete(String.format(BOARD_KEY_PREFIX, matchId));
    }
}
