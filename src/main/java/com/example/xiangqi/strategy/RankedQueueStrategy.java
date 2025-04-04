package com.example.xiangqi.strategy;

import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.repository.PlayerRepository;
import com.example.xiangqi.service.MatchService;
import org.springframework.data.redis.core.RedisTemplate;

public class RankedQueueStrategy implements QueueStrategy {
    private static final String QUEUE_KEY = "waiting_players_ranked";
    private static final String MATCH_SUCCESS = "MATCH_FOUND";

    // Giả sử có một service để lấy rank của người chơi
    private final PlayerRepository playerRepository;

    public RankedQueueStrategy(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public QueueResponse queue(Long playerId, RedisTemplate<String, String> redisTemplate, MatchService matchService) {
        // Lấy rank của người chơi hiện tại
        Integer playerRating = playerRepository.getRatingById(playerId);

        // Duyệt qua danh sách người chơi đang chờ để tìm người có rank tương đương
        Long listSize = redisTemplate.opsForList().size(QUEUE_KEY);
        String opponentId = null;

        for (int i = 0; i < listSize; i++) {
            String potentialOpponentId = redisTemplate.opsForList().index(QUEUE_KEY, i);
            if (potentialOpponentId != null) {
                Integer opponentRank = playerRepository.getRatingById(Long.valueOf(potentialOpponentId));
                if (Math.abs(playerRating - opponentRank) <= 100) { // Ghép nếu rank chênh lệch <= 1
                    opponentId = potentialOpponentId;
                    redisTemplate.opsForList().remove(QUEUE_KEY, 1, opponentId); // Xóa đối thủ khỏi queue
                    break;
                }
            }
        }

        if (opponentId != null) {
            // Match found! Create a new match
            Long matchId = matchService.createMatch(Long.valueOf(opponentId), playerId);
            return new QueueResponse(matchId, MATCH_SUCCESS);
        } else {
            // No opponent yet, add this player to the queue
            redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(playerId));
            return new QueueResponse(null, "WAITING_FOR_OPPONENT");
        }
    }
}