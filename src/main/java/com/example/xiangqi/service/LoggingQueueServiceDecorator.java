package com.example.xiangqi.service;

import com.example.xiangqi.dto.response.QueueResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LoggingQueueServiceDecorator extends QueueService {

    public LoggingQueueServiceDecorator(RedisTemplate<String, String> redisTemplate, MatchService matchService) {
        super(redisTemplate, matchService);
    }

    @Override
    public QueueResponse queue(){
        System.out.println("Logging: A new user join the queue.");
        QueueResponse result = super.queue();
        return result;
    }
}
