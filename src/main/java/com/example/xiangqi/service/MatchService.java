package com.example.xiangqi.service;

import com.example.xiangqi.dto.request.MoveRequest;
import com.example.xiangqi.dto.response.MatchResultResponse;
import com.example.xiangqi.dto.response.MatchStateResponse;
import com.example.xiangqi.dto.response.MoveResponse;
import com.example.xiangqi.dto.response.QueueResponse;
import com.example.xiangqi.entity.MatchEntity;
import com.example.xiangqi.entity.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.repository.MatchRepository;
import com.example.xiangqi.repository.PlayerRepository;
import com.example.xiangqi.util.BoardUtils;
import com.example.xiangqi.util.MoveValidator;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class MatchService {
	MatchRepository matchRepository;
	SimpMessagingTemplate messagingTemplate; // For WebSocket notifications
	PlayerRepository playerRepository;
	RedisGameService redisGameService;

	private static final String MATCH_SUCCESS = "MATCH_FOUND";
	private static final long INITIAL_TIME_MS = 60_000 * 15;

	public Long createMatch(Long player1Id, Long player2Id) {
		MatchEntity matchEntity = new MatchEntity();

		// Randomly assign colors
		boolean firstIsRed = Math.random() < 0.5;
		matchEntity.setRedPlayerEntity(firstIsRed
				? playerRepository.getReferenceById(player1Id)
				: playerRepository.getReferenceById(player2Id));
		matchEntity.setBlackPlayerEntity(firstIsRed
				? playerRepository.getReferenceById(player2Id)
				: playerRepository.getReferenceById(player1Id));

		matchRepository.save(matchEntity);

		// REDIS:
		// Initial board state
		String initialBoard = BoardUtils.getInitialBoardState();
		redisGameService.saveBoardStateJson(matchEntity.getId(), initialBoard);
		// Save player ID
		redisGameService.savePlayerId(matchEntity.getId(), firstIsRed ? player1Id : player2Id, true);
		redisGameService.savePlayerId(matchEntity.getId(), firstIsRed ? player2Id : player1Id, false);
		// Initial turn
		redisGameService.saveTurn(matchEntity.getId(), firstIsRed ? player1Id : player2Id);
		// Initial player's time-left
		redisGameService.savePlayerTimeLeft(matchEntity.getId(), INITIAL_TIME_MS, true);
		redisGameService.savePlayerTimeLeft(matchEntity.getId(), INITIAL_TIME_MS, false);
		// Initial player's ready-status
		redisGameService.savePlayerReadyStatus(matchEntity.getId(), true, false);
		redisGameService.savePlayerReadyStatus(matchEntity.getId(), false, false);

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/queue/player/" + player1Id,
				new ResponseObject("ok", "Match found.", new QueueResponse(matchEntity.getId(), MATCH_SUCCESS)));
		messagingTemplate.convertAndSend("/topic/queue/player/" + player2Id,
				new ResponseObject("ok", "Match found.", new QueueResponse(matchEntity.getId(), MATCH_SUCCESS)));

		return matchEntity.getId();
	}

	public MatchStateResponse getMatchStateById(Long matchId) {
		// Retrieve match state from Redis:
		// Get board state
		String boardStateJson = redisGameService.getBoardStateJson(matchId);
		// Get player ID
		Long redPlayerId = redisGameService.getPlayerId(matchId, true);
		Long blackPlayerId = redisGameService.getPlayerId(matchId, false);
		// Get turn
		Long turn = redisGameService.getTurn(matchId);
		// Get player's timer-left
		Long redPlayerTimeLeft = redisGameService.getPlayerTimeLeft(matchId, true);
		Long blackPlayerTimeLeft = redisGameService.getPlayerTimeLeft(matchId, false);
		// Get last-move's time
		Instant lastMoveTime = redisGameService.getLastMoveTime(matchId);

		// Return match state
		return MatchStateResponse.builder()
				.boardState(BoardUtils.boardParse(boardStateJson))
				.redPlayerId(redPlayerId)
				.blackPlayerId(blackPlayerId)
				.turn(turn)
				.redPlayerTimeLeft(redPlayerTimeLeft)
				.blackPlayerTimeLeft(blackPlayerTimeLeft)
				.lastMoveTime(lastMoveTime)
				.build();
	}

	public void ready(Long matchId) {
		// Get Jwt token from Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		// Get userId from token
		Long userId = jwt.getClaim("uid");

		// Get match player's ID
		Long redPlayerId = redisGameService.getPlayerId(matchId, true);
		Long blackPlayerId = redisGameService.getPlayerId(matchId, false);
		// If this user isn't belong to this match
		if (!userId.equals(redPlayerId) && !userId.equals(blackPlayerId))
			throw new AppException(ErrorCode.MATCH_READY_INVALID);

		// Get user's faction
		boolean isRedPlayer = redPlayerId.equals(userId);

		// Update the ready status
		redisGameService.savePlayerReadyStatus(matchId, isRedPlayer, true);

		// Acquire lock
		redisGameService.acquireMatchInitialLock(matchId);

		// Check to start match
		try {
			// Get opponent's ready-status
			Boolean opponentReadyStatus = redisGameService.getPlayerReadyStatus(matchId, !isRedPlayer);
			// Get match's start-state through last-move time
			Instant lastMoveTime = redisGameService.getLastMoveTime(matchId);

			// Start the match if the opponent is ready
			if (lastMoveTime == null && opponentReadyStatus) {
				// Initial last-move's time
				redisGameService.saveLastMoveTime(matchId, Instant.now());
				// Get match state
				MatchStateResponse matchStateResponse = getMatchStateById(matchId);
				// Notify both player: The match is start
				messagingTemplate.convertAndSend("/topic/match/player/" + redPlayerId,
						new ResponseObject("ok", "The match is start.", matchStateResponse));
				messagingTemplate.convertAndSend("/topic/match/player/" + blackPlayerId,
						new ResponseObject("ok", "The match is start.", matchStateResponse));
			}
		} finally {
			redisGameService.releaseMatchInitialLock(matchId);
		}
	}

	public void move(Long matchId, MoveRequest moveRequest) {
		// Retrieve board state from Redis
		String boardStateJson = redisGameService.getBoardStateJson(matchId);

		// Retrive match state from Redis
		String[][] boardState = BoardUtils.boardParse(boardStateJson);
		Long redPlayerId = redisGameService.getPlayerId(matchId, true);
		Long blackPlayerId = redisGameService.getPlayerId(matchId, false);
		Long turn = redisGameService.getTurn(matchId);
		Long redPlayerTimeLeft = redisGameService.getPlayerTimeLeft(matchId, true);
		Long blackPlayerTimeLeft = redisGameService.getPlayerTimeLeft(matchId, false);
		Instant lastMoveTime = redisGameService.getLastMoveTime(matchId);

		// Move validate
		if (!MoveValidator.isValidMove(boardState, redPlayerId, blackPlayerId, turn,
				moveRequest.getFrom().getRow(), moveRequest.getFrom().getCol(), moveRequest.getTo().getRow(),
				moveRequest.getTo().getCol())) {
			throw new AppException(ErrorCode.INVALID_MOVE);
		}

		// Apply move & update Redis
		applyMove(matchId, redPlayerId, blackPlayerId, turn, boardState, moveRequest, redPlayerTimeLeft, blackPlayerTimeLeft, lastMoveTime);
	}

	public MatchResultResponse resign(Long matchId) {
		// Get Jwt token from Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		// Get userId from token
		Long userId = jwt.getClaim("uid");

		// Get PlayerId
		Long redPlayerId = redisGameService.getPlayerId(matchId, true);
		Long blackPlayerId = redisGameService.getPlayerId(matchId, false);
		// Get current turn
		Long turn = redisGameService.getTurn(matchId);

		// Not in your turn
		if (!userId.equals(turn)) throw new AppException(ErrorCode.INVALID_RESIGN);

		boolean	isRed = turn.equals(redPlayerId);

		// Update match info
		MatchEntity matchEntity = matchRepository.findById(matchId)
				.orElseThrow(() -> new AppException(ErrorCode.MATCH_NOT_FOUND));
		matchEntity.setResult(isRed ? "Black Player Win" : "Red Player Win"); // Opponent wins
		matchEntity.setEndTime(Instant.now());
		matchRepository.save(matchEntity);

		// Update red's elo
		PlayerEntity redPlayerEntity = playerRepository.findById(redPlayerId)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		redPlayerEntity.setRating(isRed ? redPlayerEntity.getRating() - 10 : redPlayerEntity.getRating() + 10);
		playerRepository.save(redPlayerEntity);
		// Update black's elo
		PlayerEntity blackPlayerEntity = playerRepository.findById(blackPlayerId)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		redPlayerEntity.setRating(isRed ? blackPlayerEntity.getRating() + 10 : blackPlayerEntity.getRating() - 10);
		playerRepository.save(blackPlayerEntity);

		// Delete match state
		redisGameService.deleteBoardState(matchEntity.getId());
		redisGameService.deletePlayerId(matchEntity.getId(), true);
		redisGameService.deletePlayerId(matchEntity.getId(), false);
		redisGameService.deleteTurn(matchEntity.getId());
		redisGameService.deletePlayerTimeLeft(matchEntity.getId(), true);
		redisGameService.deletePlayerTimeLeft(matchEntity.getId(), false);
		redisGameService.deleteLastMoveTime(matchEntity.getId());
		redisGameService.deletePlayerReadyStatus(matchEntity.getId(), true);
		redisGameService.deletePlayerReadyStatus(matchEntity.getId(), false);

		messagingTemplate.convertAndSend("/topic/match/player/" + (isRed ? blackPlayerId : redPlayerId),
				new ResponseObject("ok", "You win the match", new MatchResultResponse("WIN", isRed ? blackPlayerEntity.getRating() : redPlayerEntity.getRating())));

		return new MatchResultResponse("LOSE", isRed ? redPlayerEntity.getRating() : blackPlayerEntity.getRating());
	}

	private void applyMove(Long matchId, Long redPlayerId, Long blackPlayerId, Long currentTurn, String[][] boardState, MoveRequest moveRequest, Long redPlayerTimeLeft, Long blackPlayerTimeLeft, Instant lastMoveTime) {
		// Apply the move (update board state)
		boardState[moveRequest.getTo().getRow()][moveRequest.getTo().getCol()] =
				boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()]; // Move piece
		boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()] = ""; // Clear old position

		// Convert back to JSON and save to Redis
		String updatedBoardStateJson = BoardUtils.boardSerialize(boardState);
		redisGameService.saveBoardStateJson(matchId, updatedBoardStateJson);

		// Get user's faction
		boolean isRedPlayer = currentTurn.equals(redPlayerId);

		// Switch turns
		Long nextTurn = isRedPlayer ? blackPlayerId : redPlayerId;
		redisGameService.saveTurn(matchId, nextTurn);

		// Current player's time-left
		Long currentPlayerTimeLeft = isRedPlayer ? redPlayerTimeLeft : blackPlayerTimeLeft;
		// Update player's time-left
		redisGameService.savePlayerTimeLeft(matchId,
				currentPlayerTimeLeft - (Instant.now().toEpochMilli()-lastMoveTime.toEpochMilli()), isRedPlayer);

		// Update Last Move Time
		redisGameService.saveLastMoveTime(matchId, Instant.now());

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/match/player/" + (isRedPlayer ? blackPlayerId : redPlayerId),
				new ResponseObject("ok", "Opponent player has moved.", new MoveResponse(moveRequest.getFrom(), moveRequest.getTo())));
	}
}
