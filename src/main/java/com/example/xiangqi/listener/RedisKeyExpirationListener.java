package com.example.xiangqi.listener;

import com.example.xiangqi.service.my_sql.MatchContractService;
import com.example.xiangqi.service.my_sql.MatchService;
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
    MatchContractService matchContractService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        if (key.matches("match:.*:expiration:")) {
            String[] parts = key.split(":");
            String matchId = parts[1];
            matchService.handleMatchExpiration(Long.valueOf(matchId));
        } else if (key.matches("matchContract:.*:expiration:")) {
            String[] parts = key.split(":");
            String matchContractId = parts[1];
            matchContractService.handleMatchContractExpiration(matchContractId);
        }
    }
}