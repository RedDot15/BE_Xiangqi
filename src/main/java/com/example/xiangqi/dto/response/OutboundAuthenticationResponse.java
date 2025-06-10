package com.example.xiangqi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutboundAuthenticationResponse {
	String accessToken;

	String refreshToken;

	boolean userExists;

	String registrationToken;

	String email;
}
