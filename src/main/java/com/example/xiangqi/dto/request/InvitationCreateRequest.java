package com.example.xiangqi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvitationCreateRequest {
    @NotBlank(message = "Inviter username is required.")
    String inviterUsername;

    @NotBlank(message = "Invitee username is required.")
    String inviteeUsername;
}
