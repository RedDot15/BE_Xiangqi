package com.example.xiangqi.service.my_sql;

import com.example.xiangqi.dto.request.ContractPlayerRequest;
import com.example.xiangqi.dto.request.MatchContractRequest;
import com.example.xiangqi.dto.request.QueueJoinRequest;
import com.example.xiangqi.dto.request.QueueLeaveRequest;
import com.example.xiangqi.helper.MessageObject;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    public void joinQueue(QueueJoinRequest joinRequest) {
        // Get current player's rank
        Integer myRating = playerService.getRatingById(joinRequest.getJoinerId());

        // Wait to acquire lock
        redisQueueService.acquireQueueLock();

        Long opponentId = null;
        try {
            // Browse for opponent with equivalent rank
            Long queueSize = redisQueueService.getQueueSize();
            for (int i = 0; i < queueSize; i++) {
                Long potentialOpponentId = redisQueueService.getPlayerIdByIndex(i);
                if (!potentialOpponentId.equals(joinRequest.getJoinerId())) {
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
            MatchContractRequest matchContractRequest = MatchContractRequest.builder()
                    .player1(new ContractPlayerRequest(joinRequest.getJoinerId()))
                    .player2(new ContractPlayerRequest(opponentId))
                    .build();
            String matchContractId = matchContractService.create(matchContractRequest);

            // Notify players via WebSocket
            messagingTemplate.convertAndSend("/topic/queue/player/" + opponentId,
                    new MessageObject("Match found.", matchContractId));
            messagingTemplate.convertAndSend("/topic/queue/player/" + joinRequest.getJoinerId(),
                    new MessageObject("Match found.", matchContractId));
        } else {
            // Check if playerId already exists in the queue
            List<Long> queue = redisQueueService.getAll();
            if (!queue.contains(joinRequest.getJoinerId())) {
                // No opponent yet, add this player to the queue
                redisQueueService.rightPush(joinRequest.getJoinerId());
            }
            // Notify join queue success
            messagingTemplate.convertAndSend("/topic/queue/player/" + joinRequest.getJoinerId(),
                    new MessageObject("Join queue success.", null));
        }
    }

    public void leaveQueue(QueueLeaveRequest request){
        // Wait to acquire lock
        redisQueueService.acquireQueueLock();

        try {
            // Remove my Id from queue
            redisQueueService.deletePlayerId(request.getLeaverId());
        } finally {
            // Release lock
            redisQueueService.releaseQueueLock();
        }

        // Notify leave queue success
        messagingTemplate.convertAndSend("/topic/queue/player/" + request.getLeaverId(),
                new MessageObject("Leave queue success.", null));
    }


}
