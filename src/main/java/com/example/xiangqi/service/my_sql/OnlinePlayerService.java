package com.example.xiangqi.service.my_sql;


import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.entity.my_sql.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.mapper.PlayerMapper;
import com.example.xiangqi.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
public class OnlinePlayerService {
    // Maps to store session and player information
    @NonFinal
    Map<String, PlayerInfo> sessionToPlayer = new ConcurrentHashMap<>();
    @NonFinal
    Map<String, PlayerInfo> usernameToPlayer = new ConcurrentHashMap<>();

    PlayerRepository playerRepository;
    PlayerMapper playerMapper;

    public void updateStatus(String payload, StompHeaderAccessor headerAccessor) {
        String[] parts = payload.replaceAll("\"", "").split(":");
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

    public PlayerInfo getPlayerInfo(String username) {
        return usernameToPlayer.get(username);
    }

    public PlayerResponse getPlayer(String username) {
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
