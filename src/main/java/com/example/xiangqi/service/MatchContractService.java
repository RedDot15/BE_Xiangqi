package com.example.xiangqi.service;

import com.example.xiangqi.dto.request.MatchContractRequest;
import com.example.xiangqi.entity.redis.MatchContractEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.mapper.MatchContractMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
        // Save & Return match contract ID
        redisMatchContractService.saveMatchContract(matchContractId, entity, MATCH_CONTRACT_EXPIRATION);
        // Return match contract ID
        return matchContractId;
    }

    public void accept(String matchContractId){
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get userId from token
        Long myId = jwt.getClaim("uid");

        // Get match contract
        MatchContractEntity mcEntity1 = redisMatchContractService.getMatchContract(matchContractId);
        // Match contract not found exception
        if (mcEntity1 == null)
            throw new AppException(ErrorCode.MATCH_CONTRACT_NOT_FOUND);
        if (!myId.equals(mcEntity1.getPlayer1().getId()) && !myId.equals(mcEntity1.getPlayer2().getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);

        // Get side
        boolean isPlayer1 = myId.equals(mcEntity1.getPlayer1().getId());

        // Update the accept status: true
        if (isPlayer1)
            mcEntity1.getPlayer1().setAcceptStatus(true);
        else
            mcEntity1.getPlayer2().setAcceptStatus(true);
        redisMatchContractService.updateMatchContract(matchContractId, mcEntity1);

        // Acquire lock
        redisMatchContractService.acquireMatchContractLock(matchContractId);

        // Check to start match
        try {
            // Get match contract
            MatchContractEntity mcEntity2 = redisMatchContractService.getMatchContract(matchContractId);

            // Start the match if the opponent is ready
            if (mcEntity2 != null) {
                // Get opponent's accept-status
                boolean opponentAcceptStatus = isPlayer1
                        ? mcEntity2.getPlayer2().getAcceptStatus()
                        : mcEntity2.getPlayer1().getAcceptStatus();

                if (opponentAcceptStatus) {
                    // Delete players accept status
                    redisMatchContractService.deleteMatchContract(matchContractId);

                    // Create match
                    Long matchId = matchService.createMatch(
                            mcEntity2.getPlayer1().getId(),
                            mcEntity2.getPlayer2().getId());

                    // Notify both player: The match is start
                    messagingTemplate.convertAndSend("/topic/queue/player/" + mcEntity2.getPlayer1().getId(),
                            new ResponseObject("ok", "The match is created.", matchId));
                    messagingTemplate.convertAndSend("/topic/queue/player/" + mcEntity2.getPlayer2().getId(),
                            new ResponseObject("ok", "The match is created.", matchId));
                }
            }
        } finally {
            redisMatchContractService.releaseMatchContractLock(matchContractId);
        }
    }

    public void handleMatchContractTimeout(String matchContractId) {
        // Notify both players match contract timeout
        messagingTemplate.convertAndSend("/topic/match-contract/" + matchContractId,
                new ResponseObject("ok", "Match contract timeout.", null));
    }
}
