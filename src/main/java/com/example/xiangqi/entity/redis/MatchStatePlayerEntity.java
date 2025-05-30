package com.example.xiangqi.entity.redis;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchStatePlayerEntity {
    Long id;

    String name;

    Integer rating;

    Long totalTimeLeft;
}
