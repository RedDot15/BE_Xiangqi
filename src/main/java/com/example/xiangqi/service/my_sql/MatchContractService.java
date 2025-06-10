package com.example.xiangqi.service.my_sql;

import com.example.xiangqi.dto.request.ContractAcceptRequest;
import com.example.xiangqi.dto.request.MatchContractRequest;
import com.example.xiangqi.entity.redis.MatchContractEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.MessageObject;
import com.example.xiangqi.mapper.MatchContractMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class MatchContractService {
    MatchContractMapper matchContractMapper;
    RedisMatchContractService redisMatchContractService;
    SimpMessagingTemplate messagingTemplate;
    MatchService matchService;

    private static final long MATCH_CONTRACT_EXPIRATION = 5_000 * 1;

    public String create(MatchContractRequest request) {
        // Mapping
        MatchContractEntity entity = matchContractMapper.toEntity(request);
        // Set default accept status: false
        entity.getPlayer1().setAcceptStatus(false);
        entity.getPlayer2().setAcceptStatus(false);
        // Generate match contract ID
        String matchContractId = UUID.randomUUID().toString();
        // Initial match contract
        redisMatchContractService.saveMatchContract(matchContractId, entity);
        redisMatchContractService.saveMatchContractExpiration(matchContractId, MATCH_CONTRACT_EXPIRATION);
        // Return match contract ID
        return matchContractId;
    }

    public void accept(ContractAcceptRequest request){
        // Get match contract
        MatchContractEntity mcEntity1 = redisMatchContractService.getMatchContract(request.getMatchContractId());
        // Match contract not found exception
        if (mcEntity1 == null)
            throw new AppException(ErrorCode.MATCH_CONTRACT_NOT_FOUND);
        if (!request.getAcceptorId().equals(mcEntity1.getPlayer1().getId()) && !request.getAcceptorId().equals(mcEntity1.getPlayer2().getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);

        // Get side
        boolean isPlayer1 = request.getAcceptorId().equals(mcEntity1.getPlayer1().getId());

        // Update the accept status: true
        if (isPlayer1)
            mcEntity1.getPlayer1().setAcceptStatus(true);
        else
            mcEntity1.getPlayer2().setAcceptStatus(true);
        redisMatchContractService.saveMatchContract(request.getMatchContractId(), mcEntity1);

        // Acquire lock
        redisMatchContractService.acquireMatchContractLock(request.getMatchContractId());

        // Check to start match
        try {
            // Get match contract
            MatchContractEntity mcEntity2 = redisMatchContractService.getMatchContract(request.getMatchContractId());

            // Start the match if the opponent is ready
            if (mcEntity2 != null) {
                // Get opponent's accept-status
                boolean opponentAcceptStatus = isPlayer1
                        ? mcEntity2.getPlayer2().getAcceptStatus()
                        : mcEntity2.getPlayer1().getAcceptStatus();
                // Opponent is ready
                if (opponentAcceptStatus) {
                    // Delete match contract
                    redisMatchContractService.deleteMatchContract(request.getMatchContractId());
                    redisMatchContractService.deleteMatchContractExpiration(request.getMatchContractId());

                    // Create match
                    Long matchId = matchService.createMatch(
                            mcEntity2.getPlayer1().getId(),
                            mcEntity2.getPlayer2().getId(),
                            true);

                    // Notify both player: The match is start
                    messagingTemplate.convertAndSend("/topic/match-contract/" + request.getMatchContractId(),
                            new MessageObject("The match is created.", matchId));
                }
            }
        } finally {
            redisMatchContractService.releaseMatchContractLock(request.getMatchContractId());
        }
    }

    public void handleMatchContractExpiration(String matchContractId) {
        // Delete match contract
        redisMatchContractService.deleteMatchContract(matchContractId);
        // Notify both players match contract timeout
        messagingTemplate.convertAndSend("/topic/match-contract/" + matchContractId,
                new MessageObject("Match contract timeout.", null));
    }
}
