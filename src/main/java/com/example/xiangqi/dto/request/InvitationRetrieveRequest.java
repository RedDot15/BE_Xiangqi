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
public class InvitationRetrieveRequest {
    @NotBlank(message = "Inviter username is required.")
    String inviterUsername;

    @Nullable
    String inviteeUsername;
}
