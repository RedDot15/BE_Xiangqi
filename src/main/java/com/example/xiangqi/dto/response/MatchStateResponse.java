package com.example.xiangqi.dto.response;

import lombok.*;
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

    Long redPlayerTimeLeft;

    Long blackPlayerTimeLeft;

    Instant lastMoveTime;
}
