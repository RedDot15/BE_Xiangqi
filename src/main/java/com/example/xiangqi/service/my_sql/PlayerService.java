package com.example.xiangqi.service.my_sql;

import com.example.xiangqi.dto.request.ChangePasswordRequest;
import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.dto.request.SocialPlayerRequest;
import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.entity.my_sql.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.mapper.PlayerMapper;
import com.example.xiangqi.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class PlayerService {
	PlayerRepository playerRepository;
	PlayerMapper playerMapper;
	PasswordEncoder passwordEncoder;
	AuthenticationService authenticationService;

	public List<PlayerResponse> getAll(int page, int size, String role) {
		// Define pageable
		Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "rating"));
		// Return player list
		return playerRepository.findAll(pageable, role)
				.stream().map(playerMapper::toPlayerResponse)
				.collect(Collectors.toList());
	}

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

	public Integer getRatingById(Long id) {
		return playerRepository.findRatingById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
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
			throw new AppException(ErrorCode.USER_DUPLICATE);
		}
		// Save & Return
		return playerMapper.toPlayerResponse(playerEntity);
	}

	public PlayerResponse socialRegister(SocialPlayerRequest request) {
		// Mapping socialRegisterRequest -> playerEntity
		PlayerEntity playerEntity = PlayerEntity.builder()
				.username(request.getUsername())
				.build();
		// Encode password
		playerEntity.setPassword(passwordEncoder.encode(request.getPassword()));
		// Get & Set user email
		String email = authenticationService.getUserEmailFromToken(request.getRegistrationToken());
		playerEntity.setEmail(email);
		// Add
		try {
			playerEntity = playerRepository.save(playerEntity);
		} catch (DataIntegrityViolationException e) {
			throw new AppException(ErrorCode.USER_DUPLICATE);
		}
		// Save & Return
		return playerMapper.toPlayerResponse(playerEntity);
	}

	public PlayerResponse changePassword(ChangePasswordRequest request) {
		// Get context
		SecurityContext context = SecurityContextHolder.getContext();
		String myUsername = context.getAuthentication().getName();
		// Get old
		PlayerEntity foundPlayerEntity = playerRepository.findByUsername(myUsername)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		// Old password unmatched exception
		if (!passwordEncoder.matches(request.getOldPassword(), foundPlayerEntity.getPassword()))
			throw new AppException(ErrorCode.WRONG_PASSWORD);
		// Change password
		foundPlayerEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));
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
