package com.example.xiangqi.dto.request;

import com.example.xiangqi.dto.model.Position;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoveRequest {
    @NotNull(message = "From is required.")
    Position from;

    @NotNull(message = "To is required.")
    Position to;
}
