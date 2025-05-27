package com.example.xiangqi.service;

import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class QueueService {
    RedisTemplate<String, String> redisTemplate;
    MatchService matchService;
    PlayerService playerService;
    SimpMessagingTemplate messagingTemplate;
    RedisQueueService redisQueueService;

    private static final String MATCH_SUCCESS = "MATCH_FOUND";

    private static final String QUEUE_KEY = "waitingPlayers:";

    private static final long MATCH_ACCEPT_EXPIRATION = 5_000 * 1;

    public void queue() {
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long myId = jwt.getClaim("uid");
        // Get current player's rank
        Integer playerRating = playerService.getRatingById(myId);

        // Wait to acquire lock
        redisQueueService.acquireQueueLock();

        String opponentId = null;
        try {
            // Browse for opponent with equivalent rank
            Long listSize = redisTemplate.opsForList().size(QUEUE_KEY);
            for (int i = 0; i < listSize; i++) {
                String potentialOpponentId = redisTemplate.opsForList().index(QUEUE_KEY, i);
                if (potentialOpponentId != null && !potentialOpponentId.equals(String.valueOf(myId))) {
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
            redisQueueService.releaseLock();
        }

        if (opponentId != null) {
            // Set TTL redis key to wait players accept match
            redisQueueService.savePlayerAcceptStatus(Long.valueOf(opponentId), false);
            redisQueueService.savePlayerAcceptStatus(myId, false);
            redisQueueService.saveMatchAcceptExpiration(myId, Long.valueOf(opponentId), MATCH_ACCEPT_EXPIRATION);

            // Notify players via WebSocket
            messagingTemplate.convertAndSend("/topic/queue/player/" + opponentId,
                    new ResponseObject("ok", "Match found.", myId));
            messagingTemplate.convertAndSend("/topic/queue/player/" + myId,
                    new ResponseObject("ok", "Match found.", opponentId));
        } else {
            // Check if playerId already exists in the queue
            List<String> queue = redisTemplate.opsForList().range(QUEUE_KEY, 0, -1);
            if (queue != null && !queue.contains(String.valueOf(myId))) {
                // No opponent yet, add this player to the queue
                redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(myId));
            }
        }
    }

    public void acceptMatch(Long opponentId){
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get userId from token
        Long myId = jwt.getClaim("uid");

        // No queue between 2 player exception
        if (redisQueueService.getMatchAcceptExpiration(opponentId, myId) == null)
            throw new AppException(ErrorCode.ACCEPT_MATCH_INVALID);
        // Update the accept status
        redisQueueService.savePlayerAcceptStatus(myId, true);

        // Acquire lock
        redisQueueService.acquireMatchAcceptLock(myId, opponentId);

        // Check to start match
        try {
            // Get opponent's accept-status
            Boolean opponentAcceptStatus = redisQueueService.getPlayerAcceptStatus(opponentId);

            // Start the match if the opponent is ready
            if (opponentAcceptStatus != null && opponentAcceptStatus) {
                // Delete players accept status
                redisQueueService.deletePlayerAcceptStatus(opponentId);
                redisQueueService.deletePlayerAcceptStatus(myId);
                // Delete match accept expiration
                redisQueueService.deleteMatchAcceptExpiration(opponentId, myId);

                // Create match
                Long matchId = matchService.createMatch(opponentId, myId);

                // Notify both player: The match is start
                messagingTemplate.convertAndSend("/topic/queue/player/" + myId,
                        new ResponseObject("ok", "The match is created.", new QueueResponse(matchId, MATCH_SUCCESS)));
                messagingTemplate.convertAndSend("/topic/queue/player/" + opponentId,
                        new ResponseObject("ok", "The match is created.", new QueueResponse(matchId, MATCH_SUCCESS)));
            }
        } finally {
            redisQueueService.releaseMatchAcceptLock(myId, opponentId);
        }
    }

    public void handleMatchAcceptTimeout(Long player1Id, Long player2Id) {
        // Delete players accept status
        redisQueueService.deletePlayerAcceptStatus(player1Id);
        redisQueueService.deletePlayerAcceptStatus(player2Id);

        // Notify both player match accept timeout
        messagingTemplate.convertAndSend("/topic/queue/player/" + player1Id,
                new ResponseObject("ok", "Match accept timeout.", null));
        messagingTemplate.convertAndSend("/topic/queue/player/" + player2Id,
                new ResponseObject("ok", "Match accept timeout.", null));
    }

    public void unqueue(){
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long playerId = jwt.getClaim("uid");

        // Wait to acquire lock
        redisQueueService.acquireQueueLock();

        try {
            // Remove my Id from queue
            Long removeCount = redisTemplate.opsForList().remove(QUEUE_KEY, 1, String.valueOf(playerId));
            // Not found exception
            if (removeCount == 0)
                throw new AppException(ErrorCode.UNQUEUE_INVALID);
        } finally {
            // Release lock
            redisQueueService.releaseLock();
        }
    }


}
