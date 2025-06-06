package com.example.xiangqi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchStatePlayerResponse {
    Long id;

    String name;

    Integer rating;

    Long totalTimeLeft;
}
