package com.example.xiangqi.listener;

import com.example.xiangqi.service.MatchService;
import com.example.xiangqi.service.QueueService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisKeyExpirationListener implements MessageListener {
    MatchService matchService;
    QueueService queueService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        if (key.matches("match:.*:turnTimeExpiration:") || key.matches("match:.*:totalTimeExpiration:")) {
            String[] parts = key.split(":");
            String matchId = parts[1];
            matchService.handleTimeout(Long.valueOf(matchId));
        } else if (key.matches("player1Id:.*:player2Id:.*:matchAcceptExpiration:")) {
            String[] parts = key.split(":");
            String player1Id = parts[1];
            String player2Id = parts[3];
            queueService.handleMatchAcceptTimeout(Long.valueOf(player1Id), Long.valueOf(player2Id));
        }
    }
}