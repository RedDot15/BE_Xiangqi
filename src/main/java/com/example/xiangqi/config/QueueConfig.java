package com.example.xiangqi.config;

import com.example.xiangqi.repository.PlayerRepository;
import com.example.xiangqi.service.PlayerService;
import com.example.xiangqi.strategy.NormalQueueStrategy;
import com.example.xiangqi.strategy.QueueStrategy;
import com.example.xiangqi.strategy.RankedQueueStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    @Bean("normal")
    public QueueStrategy normalQueueStrategy() {
        return new NormalQueueStrategy();
    }

    @Bean("ranked")
    public QueueStrategy rankedQueueStrategy(PlayerRepository playerRepository) {
        return new RankedQueueStrategy(playerRepository);
    }
}