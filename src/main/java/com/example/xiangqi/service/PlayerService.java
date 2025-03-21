package com.example.xiangqi.service;

import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.entity.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.mapper.PlayerMapper;
import com.example.xiangqi.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class PlayerService {
	PlayerRepository playerRepository;
	PlayerMapper playerMapper;
	PasswordEncoder passwordEncoder;

	public PlayerResponse getMyInfo() {
		// Get context
		SecurityContext context = SecurityContextHolder.getContext();
		String username = context.getAuthentication().getName();
		// Fetch
		PlayerEntity playerEntity = playerRepository
				.findByUsername(username)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		// Return
		return playerMapper.toPlayerResponse(playerEntity);
	}

	public PlayerResponse register(PlayerRequest playerRequest) {
		// Mapping userRequest -> userEntity
		PlayerEntity playerEntity = playerMapper.toPlayerEntity(playerRequest);
		// Encode password
		playerEntity.setPassword(passwordEncoder.encode(playerRequest.getPassword()));
		// Add
		try {
			playerEntity = playerRepository.save(playerEntity);
		} catch (DataIntegrityViolationException e) {
			throw new AppException(ErrorCode.USERNAME_DUPLICATE);
		}
		// Save & Return
		return playerMapper.toPlayerResponse(playerEntity);
	}

	@PreAuthorize(
			"hasRole('ADMIN') or (#request.id == authentication.principal.claims['uid'])")
	public PlayerResponse changePassword(PlayerRequest playerRequest) {
		// Get old
		PlayerEntity foundPlayerEntity =
				playerRepository.findById(playerRequest.getId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		// Change password
		foundPlayerEntity.setPassword(passwordEncoder.encode(playerRequest.getPassword()));
		// Save & Return
		return playerMapper.toPlayerResponse(playerRepository.save(foundPlayerEntity));
	}

	@PreAuthorize("hasRole('ADMIN') or (#id == authentication.principal.claims['uid'])")
	public Long delete(Long id) {
		// Fetch & Not found/deleted exception
		PlayerEntity playerEntity =
				playerRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		// Delete
		playerRepository.delete(playerEntity);
		// Return ID
		return id;
	}

}
