package com.example.xiangqi.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QueueLeaveRequest {
    @NotNull(message = "Leaver ID is required.")
    Long leaverId;
}
