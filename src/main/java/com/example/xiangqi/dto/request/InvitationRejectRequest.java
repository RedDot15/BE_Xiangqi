package com.example.xiangqi.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvitationRejectRequest {
    @Nullable
    String inviterUsername;

    @NotBlank(message = "Invitee username is required.")
    String inviteeUsername;
}
