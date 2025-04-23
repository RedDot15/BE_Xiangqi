package com.example.xiangqi.listener;

import com.example.xiangqi.service.MatchService;
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

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        if (key.matches("match:.*:turnTimeExpiration:") || key.matches("match:.*:totalTimeExpiration:")) {
            String[] parts = key.split(":");
            String matchId = parts[1];
            matchService.handleTimeout(Long.valueOf(matchId));
        }
    }
}