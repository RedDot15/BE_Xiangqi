package com.example.xiangqi.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvitationAcceptRequest {
    @NotBlank(message = "Inviter username is required.")
    String inviterUsername;

    @NotBlank(message = "Invitee username is required.")
    String inviteeUsername;
}
