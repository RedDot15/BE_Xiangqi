package com.example.xiangqi.strategy;

import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.service.MatchService;
import org.springframework.data.redis.core.RedisTemplate;

public class NormalQueueStrategy implements QueueStrategy {
    private static final String QUEUE_KEY = "waiting_players_normal";
    private static final String MATCH_SUCCESS = "MATCH_FOUND";

    @Override
    public QueueResponse queue(Long playerId, RedisTemplate<String, String> redisTemplate, MatchService matchService) {
        // Check if another player is already waiting
        String opponentId = redisTemplate.opsForList().leftPop(QUEUE_KEY);

        if (opponentId != null) {
            // Match found! Create a new match
            Long matchId = matchService.createMatch(Long.valueOf(opponentId), playerId);
            return new QueueResponse(matchId, MATCH_SUCCESS);
        } else {
            // No opponent yet, add this player to the queue
            redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(playerId));
            return new QueueResponse(null, "WAITING_FOR_OPPONENT");
        }
    }
}
