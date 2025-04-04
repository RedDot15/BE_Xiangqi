package com.example.xiangqi.dto.response;

import com.example.xiangqi.dto.model.Position;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoveResponse {
    Position from;

    Position to;
}
