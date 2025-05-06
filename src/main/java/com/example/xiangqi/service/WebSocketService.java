package com.example.xiangqi.service;


import com.example.xiangqi.dto.request.ChatRequest;
import com.example.xiangqi.dto.response.ChatResponse;
import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.entity.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.mapper.PlayerMapper;
import com.example.xiangqi.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
public class WebSocketService {
    // Maps to store session and player information
    @NonFinal
    Map<String, PlayerInfo> sessionToPlayer = new ConcurrentHashMap<>();
    @NonFinal
    Map<String, PlayerInfo> usernameToPlayer = new ConcurrentHashMap<>();
    // Maps to store invitations
    Map<String, List<String>> invitations = new ConcurrentHashMap<>();

    PlayerRepository playerRepository;
    PlayerMapper playerMapper;
    SimpMessagingTemplate messagingTemplate;
    MatchService matchService;

    private static final String MATCH_SUCCESS = "MATCH_FOUND";

    public void updateStatus(String payload, StompHeaderAccessor headerAccessor) {
        String[] parts = payload.split(":");
        if (parts.length >= 4) {
            Long userId = Long.valueOf(parts[1]);
            String command = parts[2];
            String status = parts[3];
            String sessionId = headerAccessor.getSessionId();

            // Get player
            PlayerEntity playerEntity = playerRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            // Get player username
            String playerUsername = playerEntity.getUsername();
            // Get status
            PlayerStatus playerStatus = PlayerStatus.valueOf(status);

            if ("STATUS".equals(command)) {
                // Get playerInfo
                PlayerInfo playerInfo = sessionToPlayer.get(sessionId);
                if (playerInfo == null) {
                    // Define playerInfo
                    playerInfo = new PlayerInfo(playerUsername, playerStatus, userId);
                    // Save new player session to list
                    sessionToPlayer.put(sessionId, playerInfo);
                    usernameToPlayer.put(playerUsername, playerInfo);
                } else {
                    // Set new status
                    playerInfo.setStatus(playerStatus);
                }
            }
        }
    }

    public void handleChatMessage(ChatRequest request) {
        // Send invitation to opponent
        messagingTemplate.convertAndSend("/topic/chat/match/" + request.getMatchId(),
                new ResponseObject("ok", "Message received.", new ChatResponse(request.getSender(), request.getMessage(), Instant.now())));
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        // Get session ID
        String sessionId = event.getSessionId();
        // Remove session
        PlayerInfo playerInfo = sessionToPlayer.remove(sessionId);
        if (playerInfo != null) {
            usernameToPlayer.remove(playerInfo.getUsername());
        }
    }

    public PlayerResponse findPlayer(String username) {
        // Get context
        SecurityContext context = SecurityContextHolder.getContext();
        String myUsername = context.getAuthentication().getName();
        // Self finding exception
        if (username.equals(myUsername))
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Find player in session list
        PlayerInfo playerInfo = usernameToPlayer.get(username);
        // Not found exception
        if (playerInfo == null)
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        // Get player's info
        PlayerEntity playerEntity = playerRepository.findById(playerInfo.getPlayerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        // Return
        return playerMapper.toPlayerResponse(playerEntity);
    }

    public void invite(String username) {
        // Get context
        SecurityContext context = SecurityContextHolder.getContext();
        String myUsername = context.getAuthentication().getName();
        // Self finding exception
        if (username.equals(myUsername))
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Find player in session list
        PlayerInfo opponentPlayerInfo = usernameToPlayer.get(username);
        // Not found exception
        if (opponentPlayerInfo == null)
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Opponent status invalid
        if (opponentPlayerInfo.getStatus().equals(PlayerStatus.IN_MATCH))
            throw new AppException(ErrorCode.OPPONENT_STATUS_IN_MATCH);
        if (opponentPlayerInfo.getStatus().equals(PlayerStatus.QUEUE))
            throw new AppException(ErrorCode.OPPONENT_STATUS_QUEUE);

        // Save invitation
        // Get invitee's invitation list or create new one
        invitations.computeIfAbsent(myUsername, k -> new CopyOnWriteArrayList<>());
        // Invitation exists exception
        if (invitations.get(myUsername).contains(username)) {
            throw new AppException(ErrorCode.INVITATION_ALREADY_EXISTS); // Ngăn gửi lời mời trùng
        }
        // Add the invitation
        invitations.get(myUsername).add(username);

        // Send invitation to opponent
        messagingTemplate.convertAndSend("/topic/invite/player/" + opponentPlayerInfo.getPlayerId(),
                new ResponseObject("ok", "A new invitation received.", myUsername));
    }

    public void unInvite(String username) {
        // Get context
        SecurityContext context = SecurityContextHolder.getContext();
        String myUsername = context.getAuthentication().getName();

        // Get invitation list
        List<String> opponentUsernameList = invitations.get(myUsername);
        if (opponentUsernameList == null) return;

        // Delete every invitation if username null/empty
        if (username == null || username.isEmpty()) {
            for (String opponentUsername : opponentUsernameList) {
                // Remove invitation
                opponentUsernameList.remove(opponentUsername);
                // Get player info
                PlayerInfo opponentPlayerInfo = usernameToPlayer.get(opponentUsername);
                // Send invitation cancel to opponent
                messagingTemplate.convertAndSend("/topic/invite/player/" + opponentPlayerInfo.getPlayerId(),
                        new ResponseObject("ok", "Invitation canceled.", myUsername));
            }
            return;
        }

        // Self finding exception
        if (username.equals(myUsername))
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Remove invitation
        opponentUsernameList.remove(username);
        // Get player info
        PlayerInfo opponentPlayerInfo = usernameToPlayer.get(username);
        // Send invitation cancel to opponent
        messagingTemplate.convertAndSend("/topic/invite/player/" + opponentPlayerInfo.getPlayerId(),
                new ResponseObject("ok", "Invitation canceled.", myUsername));
    }

    public void acceptInvitation(String username) {
        // Get context
        SecurityContext context = SecurityContextHolder.getContext();
        String myUsername = context.getAuthentication().getName();
        // Self finding exception
        if (username.equals(myUsername))
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Get invitation list
        List<String> opponentUsernameList = invitations.get(username);
        // Remove invitation
        boolean invitationExists = opponentUsernameList.remove(myUsername);
        // Invitation not found exception
        if (!invitationExists)
            throw new AppException(ErrorCode.INVITATION_NOT_FOUND);

        // Get player info
        PlayerInfo opponentPlayerInfo = usernameToPlayer.get(username);
        PlayerInfo myPlayerInfo = usernameToPlayer.get(myUsername);

        // Create match
        Long matchId = matchService.createMatch(opponentPlayerInfo.getPlayerId(), myPlayerInfo.getPlayerId());

        // Send invitation accept to opponent
        messagingTemplate.convertAndSend("/topic/invite/player/" + opponentPlayerInfo.getPlayerId(),
                new ResponseObject("ok", "INVITATION_ACCEPTED", new QueueResponse(matchId, MATCH_SUCCESS)));
        messagingTemplate.convertAndSend("/topic/invite/player/" + myPlayerInfo.getPlayerId(),
                new ResponseObject("ok", "CUSTOM_MATCH_CREATED", new QueueResponse(matchId, MATCH_SUCCESS)));
    }

    public void rejectInvitation(String username) {
        // Get context
        SecurityContext context = SecurityContextHolder.getContext();
        String myUsername = context.getAuthentication().getName();
        // Self finding exception
        if (username.equals(myUsername))
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Get invitation list
        List<String> opponentUsernameList = invitations.get(username);
        // Remove invitation
        boolean invitationExists = opponentUsernameList.remove(myUsername);
        // Invitation not found exception
        if (!invitationExists)
            throw new AppException(ErrorCode.INVITATION_NOT_FOUND);

        // Get player info
        PlayerInfo opponentPlayerInfo = usernameToPlayer.get(username);

        // Send invitation accept to opponent
        messagingTemplate.convertAndSend("/topic/invite/player/" + opponentPlayerInfo.getPlayerId(),
                new ResponseObject("ok", "INVITATION_REJECTED", null));

    }

    // Enum for player status
    public enum PlayerStatus {
        IDLE, QUEUE, IN_MATCH
    }

    // Class to store player info
    @Getter
    @AllArgsConstructor
    public static class PlayerInfo {
        private final String username;

        @Setter
        private PlayerStatus status;

        private final Long playerId;
    }
}
