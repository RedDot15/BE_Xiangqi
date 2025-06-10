package com.example.xiangqi.service.my_sql;

import com.example.xiangqi.dto.request.AuthenticationRequest;
import com.example.xiangqi.dto.request.ExchangeTokenRequest;
import com.example.xiangqi.dto.request.OutboundAuthenticationRequest;
import com.example.xiangqi.dto.request.RefreshRequest;
import com.example.xiangqi.dto.response.*;
import com.example.xiangqi.entity.my_sql.UserEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.repository.UserRepository;
import com.example.xiangqi.service.http_client.OutboundIdentityClient;
import com.example.xiangqi.service.http_client.OutboundUserInfoClient;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
public class AuthenticationService {
	PasswordEncoder passwordEncoder;
	UserRepository userRepository;
	RedisAuthService redisAuthService;
	TokenService tokenService;
	OutboundIdentityClient outboundIdentityClient;
	OutboundUserInfoClient outboundUserClient;

	@NonFinal
	@Value("${jwt.refreshable-duration}")
	Long REFRESHABLE_DURATION;

	@NonFinal
	@Value("${spring.security.oauth2.client.google.client-id}")
	String CLIENT_ID;

	@NonFinal
	@Value("${spring.security.oauth2.client.google.client-secret}")
	String CLIENT_SECRET;

	private static final String GRANT_TYPE = "authorization_code";
	private static final long REGISTRATRION_TOKEN_EXPIRATION = 120_000 * 1;

	public String getUserEmailFromToken(String token) {
		return redisAuthService.getRegistrationTokenKey(token);
	}

	public OutboundAuthenticationResponse outboundAuthenticate(OutboundAuthenticationRequest request){
		ExchangeTokenResponse response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
				.code(request.getAuthorizationCode())
				.clientId(CLIENT_ID)
				.clientSecret(CLIENT_SECRET)
				.redirectUri(request.getRedirectUri())
				.grantType(GRANT_TYPE)
				.build());

		// Get user info
		String authorizationHeader = "Bearer " + response.getAccessToken();
		OutboundUserInfoResponse userInfo = outboundUserClient.getUserInfo("json", authorizationHeader);

		// Onboard user
		Optional<UserEntity> optionalUser = userRepository.findByEmail(userInfo.getEmail());
		if (optionalUser.isPresent()) {
			// Get user
			UserEntity entity = optionalUser.get();
			// Generate token
			String uuid = UUID.randomUUID().toString();
			String accessToken = tokenService.generateToken(entity, false, uuid);
			String refreshToken = tokenService.generateToken(entity, true, uuid);
			// Return
			return OutboundAuthenticationResponse.builder()
					.accessToken(accessToken)
					.refreshToken(refreshToken)
					.userExists(true)
					.build();
		}

		// Generate & Save registration token
		String registrationToken = UUID.randomUUID().toString();
		redisAuthService.saveRegistrationTokenKey(registrationToken, userInfo.getEmail(),REGISTRATRION_TOKEN_EXPIRATION);
		// Return
		return OutboundAuthenticationResponse.builder()
				.userExists(false)
				.registrationToken(registrationToken)
				.email(userInfo.getEmail())
				.build();
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		// Fetch
		UserEntity userEntity = userRepository
				.findByUsername(request.getUsername())
				.orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
		// Authenticate
		boolean authenticated = passwordEncoder.matches(request.getPassword(), userEntity.getPassword());
		if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);
		// Generate token
		String uuid = UUID.randomUUID().toString();
		String refreshToken = tokenService.generateToken(userEntity, true, uuid);
		String accessToken = tokenService.generateToken(userEntity, false, uuid);
		// Return token
		return AuthenticationResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}

	public RefreshResponse refresh(RefreshRequest request) {
		// Verify token
		try {
			Jwt jwt = tokenService.verifyToken(request.getRefreshToken(), true);
			// Get token information
			UserEntity userEntity = userRepository
					.findByUsername(jwt.getSubject())
					.orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
			String jti = jwt.getClaim("jti");
			Date expiryTime = Date.from(jwt.getClaim("exp"));
			// Build & Save invalid token
			redisAuthService.saveInvalidatedTokenExpirationKey(jti, expiryTime.toInstant().toEpochMilli());
			// Generate new token
			String uuid = UUID.randomUUID().toString();
			String refreshToken = tokenService.generateToken(userEntity, true, uuid);
			String accessToken = tokenService.generateToken(userEntity, false, uuid);
			// Return token
			return RefreshResponse.builder()
					.accessToken(accessToken)
					.refreshToken(refreshToken)
					.build();
		} catch (JwtException e) {
			throw new AppException(ErrorCode.UNAUTHENTICATED);
		}
	}

	public void logout() {
		// Get Jwt token from Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		// Get token information
		String jti = jwt.getClaim("rid");
		Instant expiryTime = Instant.from(jwt.getClaim("iat")).plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS);
		// Save invalid token
		redisAuthService.saveInvalidatedTokenExpirationKey(jti, expiryTime.toEpochMilli());
	}
}
