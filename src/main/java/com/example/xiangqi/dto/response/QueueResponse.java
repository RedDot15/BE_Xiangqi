package com.example.xiangqi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueResponse {
    Long matchId;

    String status; // "WAITING_FOR_OPPONENT" or "MATCH_FOUND"
}
