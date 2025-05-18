package com.example.xiangqi.service;

import com.example.xiangqi.dto.request.AuthenticationRequest;
import com.example.xiangqi.dto.request.RefreshRequest;
import com.example.xiangqi.dto.response.AuthenticationResponse;
import com.example.xiangqi.dto.response.RefreshResponse;
import com.example.xiangqi.entity.UserEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.repository.UserRepository;
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

	@NonFinal
	@Value("${jwt.refreshable-duration}")
	Long REFRESHABLE_DURATION;

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
				.authenticated(true)
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
