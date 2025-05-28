package com.example.xiangqi.service;

import com.example.xiangqi.dto.request.ContractPlayerRequest;
import com.example.xiangqi.dto.request.MatchContractRequest;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
    PlayerService playerService;
    SimpMessagingTemplate messagingTemplate;
    RedisQueueService redisQueueService;
    MatchContractService matchContractService;

    public void queue() {
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long myId = jwt.getClaim("uid");
        // Get current player's rank
        Integer myRating = playerService.getRatingById(myId);

        // Wait to acquire lock
        redisQueueService.acquireQueueLock();

        Long opponentId = null;
        try {
            // Browse for opponent with equivalent rank
            Long queueSize = redisQueueService.getQueueSize();
            for (int i = 0; i < queueSize; i++) {
                Long potentialOpponentId = redisQueueService.getPlayerIdByIndex(i);
                if (!potentialOpponentId.equals(myId)) {
                    Integer opponentRank = playerService.getRatingById(potentialOpponentId);
                    // Match if opponent's rank is equivalent
                    if (Math.abs(myRating - opponentRank) <= 100) {
                        opponentId = potentialOpponentId;
                        // Remove opponent's ID from queue
                        redisQueueService.deletePlayerId(opponentId);
                        break;
                    }
                }
            }
        } finally {
            // Always release the lock
            redisQueueService.releaseQueueLock();
        }

        if (opponentId != null) {
            // Save new match-contract
            MatchContractRequest request = MatchContractRequest.builder()
                    .player1(new ContractPlayerRequest(myId))
                    .player2(new ContractPlayerRequest(opponentId))
                    .build();
            String matchContractId = matchContractService.create(request);

            // Notify players via WebSocket
            messagingTemplate.convertAndSend("/topic/queue/player/" + opponentId,
                    new ResponseObject("ok", "Match found.", matchContractId));
            messagingTemplate.convertAndSend("/topic/queue/player/" + myId,
                    new ResponseObject("ok", "Match found.", matchContractId));
        } else {
            // Check if playerId already exists in the queue
            List<Long> queue = redisQueueService.getAll();
            if (!queue.contains(myId)) {
                // No opponent yet, add this player to the queue
                redisQueueService.rightPush(myId);
            }
        }
    }


    public void unqueue(){
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get playerId from token
        Long myId = jwt.getClaim("uid");

        // Wait to acquire lock
        redisQueueService.acquireQueueLock();

        try {
            // Remove my Id from queue
            redisQueueService.deletePlayerId(myId);;
        } finally {
            // Release lock
            redisQueueService.releaseQueueLock();
        }
    }


}
