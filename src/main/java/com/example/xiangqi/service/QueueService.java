package com.example.xiangqi.service;

import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.strategy.QueueStrategy;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class QueueService {
    RedisTemplate<String, String> redisTemplate;
    MatchService matchService;

    Map<String, QueueStrategy> strategies;

    @Autowired
    public QueueService(Map<String, QueueStrategy> strategies, MatchService matchService, RedisTemplate<String, String> redisTemplate) {
        this.strategies = strategies;
        this.matchService = matchService;
        this.redisTemplate = redisTemplate;
    }

    private static final String QUEUE_KEY = "waiting_players";

    public QueueResponse queue(Integer queueType) {
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long playerId = jwt.getClaim("uid");

        // Chọn strategy dựa trên queueType
        QueueStrategy strategy = strategies.get(queueType.equals(1) ? "normal" : "ranked");
        if (strategy == null) {
            throw new IllegalArgumentException("Loại queue không hợp lệ: " + queueType);
        }

        return strategy.queue(playerId, redisTemplate, matchService);
    }

    public void unqueue(){
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long playerId = jwt.getClaim("uid");

        // Pop my Id from queue
        Long poppedId;
        try {
            poppedId = Long.valueOf(Objects.requireNonNull(redisTemplate.opsForList().rightPop(QUEUE_KEY)));
        } catch (NullPointerException e) {
            throw new AppException(ErrorCode.EMPTY_QUEUE);
        }

        // Check if the popped Id is mine
        if (!playerId.equals(poppedId))
            throw new AppException(ErrorCode.UNQUEUE_INVALID);
    }
}
