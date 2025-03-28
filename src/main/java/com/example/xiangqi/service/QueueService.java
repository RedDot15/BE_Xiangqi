package com.example.xiangqi.service;

import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class QueueService {
    RedisTemplate<String, String> redisTemplate;
    MatchService matchService;

    private static final String QUEUE_KEY = "waiting_players";
    private static final String MATCH_SUCCESS = "MATCH_FOUND";

    public QueueResponse queue() {
//        // Get Jwt token from Context
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        Jwt jwt = (Jwt) authentication.getPrincipal();
//        // Get playerId from token
//        Long playerId = jwt.getClaim("uid");

        // Check if another player is already waiting
        String opponentId = redisTemplate.opsForList().leftPop(QUEUE_KEY);

        if (opponentId != null) {
            // Match found! Create a new match
            Long matchId = matchService.createMatch(1L, 1L);
            return new QueueResponse(matchId, MATCH_SUCCESS);
        } else {
            // No opponent yet, add this player to the queue
            redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(1L));
            return new QueueResponse(null, "WAITING_FOR_OPPONENT");
        }
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
