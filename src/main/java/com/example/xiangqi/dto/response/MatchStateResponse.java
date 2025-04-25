package com.example.xiangqi.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchStateResponse {
    String[][] boardState;

    Long redPlayerId;

    Long blackPlayerId;

    Long turn;

    String redPlayerName;

    String blackPlayerName;

    Integer redPlayerRating;

    Integer blackPlayerRating;

    Long redPlayerTimeLeft;

    Long blackPlayerTimeLeft;

    Instant lastMoveTime;
}
