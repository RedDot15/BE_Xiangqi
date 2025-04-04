package com.example.xiangqi.strategy;

import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.service.MatchService;
import org.springframework.data.redis.core.RedisTemplate;

public interface QueueStrategy {
    QueueResponse queue(Long playerId, RedisTemplate<String, String> redisTemplate, MatchService matchService);
}
