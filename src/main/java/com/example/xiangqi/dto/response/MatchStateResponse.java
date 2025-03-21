package com.example.xiangqi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchStateResponse {
    String[][] boardState;

    Long redPlayerId;

    Long blackPlayerId;

    Long turn;
}
