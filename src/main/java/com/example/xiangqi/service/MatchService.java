package com.example.xiangqi.service;

import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.entity.MatchEntity;
import com.example.xiangqi.entity.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.mapper.PlayerMapper;
import com.example.xiangqi.repository.MatchRepository;
import com.example.xiangqi.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class MatchService {
	MatchRepository matchRepository;
	SimpMessagingTemplate messagingTemplate; // For WebSocket notifications
	PlayerRepository playerRepository;

	private static final String MATCH_SUCCESS = "MATCH_FOUND";

	public Long createMatch(Long player1Id, Long player2Id) {
		MatchEntity matchEntity = new MatchEntity();
		matchEntity.setRedPlayerEntity(playerRepository.getReferenceById(player1Id));
		matchEntity.setBlackPlayerEntity(playerRepository.getReferenceById(player2Id));
		matchRepository.save(matchEntity);

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/queue/" + player1Id, new QueueResponse(matchEntity.getId(), MATCH_SUCCESS));
		messagingTemplate.convertAndSend("/topic/queue/" + player2Id, new QueueResponse(matchEntity.getId(), MATCH_SUCCESS));

		return matchEntity.getId();
	}


}
