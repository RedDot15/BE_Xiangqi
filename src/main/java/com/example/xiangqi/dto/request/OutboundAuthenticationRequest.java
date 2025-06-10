package com.example.xiangqi.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboundAuthenticationRequest {
    @NotBlank(message = "Authorization code is required.")
    String authorizationCode;

    @NotBlank(message = "Redirect URI is required.")
    String redirectUri;
}
