package com.example.xiangqi.service;

import com.example.xiangqi.dto.model.Position;
import com.example.xiangqi.dto.request.CreateAIMatchRequest;
import com.example.xiangqi.dto.request.MoveRequest;
import com.example.xiangqi.dto.response.*;
import com.example.xiangqi.entity.MatchEntity;
import com.example.xiangqi.entity.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.dto.response.PageResponse;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.mapper.MatchMapper;
import com.example.xiangqi.repository.MatchRepository;
import com.example.xiangqi.repository.PlayerRepository;
import com.example.xiangqi.util.BoardUtils;
import com.example.xiangqi.util.MoveValidator;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class MatchService {
	MatchRepository matchRepository;
	SimpMessagingTemplate messagingTemplate;
	PlayerRepository playerRepository;
	RedisMatchService redisMatchService;
	MatchMapper matchMapper;

	private static final long PLAYER_TOTAL_TIME_LEFT = 60_000 * 15;
	private static final long PLAYER_TURN_TIME_EXPIRATION = 60_000 * 1;
	private static final long PLAYER_TOTAL_TIME_EXPIRATION = 60_000 * 15;

	public PageResponse<MatchResponse> getAllFinished(int page, int size, Long userId) {
		// Define pageable
		Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "startTime"));
		// Find all finished match
		Page<MatchEntity> matchEntityPage = matchRepository.findAllFinished(pageable, userId);
		// Mapping to match response list
		List<MatchResponse> matchResponseList = matchEntityPage.getContent()
				.stream().map(matchMapper::toResponse)
				.collect(Collectors.toList());

		return new PageResponse<>(matchResponseList, matchEntityPage.getPageable(), matchEntityPage.getTotalElements());
	}

	public Long createMatch(Long player1Id, Long player2Id) {
		MatchEntity matchEntity = new MatchEntity();

		// Find the two players
		PlayerEntity player1 = playerRepository.findById(player1Id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		PlayerEntity player2 = playerRepository.findById(player2Id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		// Randomly assign colors
		boolean firstIsRed = Math.random() < 0.5;
		matchEntity.setRedPlayerEntity(firstIsRed ? player1 : player2);
		matchEntity.setBlackPlayerEntity(firstIsRed ? player2: player1);

		matchRepository.save(matchEntity);

		// REDIS:
		// Initial board state
		String initialBoard = BoardUtils.getInitialBoardState();
		redisMatchService.saveBoardStateJson(matchEntity.getId(), initialBoard);
		// Save player ID
		redisMatchService.savePlayerId(matchEntity.getId(), firstIsRed ? player1Id : player2Id, true);
		redisMatchService.savePlayerId(matchEntity.getId(), firstIsRed ? player2Id : player1Id, false);
		// Initial turn
		redisMatchService.saveTurn(matchEntity.getId(), firstIsRed ? player1Id : player2Id);
		// Initial player name
		redisMatchService.savePlayerName(matchEntity.getId(), firstIsRed ? player1.getUsername() : player2.getUsername(), true);
		redisMatchService.savePlayerName(matchEntity.getId(), firstIsRed ? player2.getUsername() : player1.getUsername(), false);
		// Initial player rating
		redisMatchService.savePlayerRating(matchEntity.getId(), firstIsRed ? player1.getRating() : player2.getRating(), true);
		redisMatchService.savePlayerRating(matchEntity.getId(), firstIsRed ? player2.getRating() : player1.getRating(), false);
		// Initial player's total time-left
		redisMatchService.savePlayerTotalTimeLeft(matchEntity.getId(), PLAYER_TOTAL_TIME_LEFT, true);
		redisMatchService.savePlayerTotalTimeLeft(matchEntity.getId(), PLAYER_TOTAL_TIME_LEFT, false);
		// Initial player's ready-status
		redisMatchService.savePlayerReadyStatus(matchEntity.getId(), true, false);
		redisMatchService.savePlayerReadyStatus(matchEntity.getId(), false, false);
		// Initial player's turn time-expiration
		redisMatchService.savePlayerTurnTimeExpiration(matchEntity.getId(), PLAYER_TURN_TIME_EXPIRATION);

		return matchEntity.getId();
	}

	public Long createMatchWithAI(CreateAIMatchRequest request) {
		MatchEntity matchEntity = new MatchEntity();

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		Long playerId = jwt.getClaim("uid");

		String mode = request.getMode();

		Optional<Long> aiIdOptional = playerRepository.findIdByRole(mode); // Lấy mode từ request
		Long aiId = aiIdOptional.orElseThrow(() -> new RuntimeException(mode + " player not found"));

		PlayerEntity player1 = playerRepository.findById(playerId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		PlayerEntity player2 = playerRepository.findById(aiId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		// Gán người chơi là Red, AI là Black
		matchEntity.setRedPlayerEntity(playerRepository.getReferenceById(playerId));
		matchEntity.setBlackPlayerEntity(playerRepository.getReferenceById(aiId)); // AI

		matchRepository.save(matchEntity);

		// REDIS: Board, Turn, Thời gian...
		String initialBoard = BoardUtils.getInitialBoardState();
		redisMatchService.saveBoardStateJson(matchEntity.getId(), initialBoard);

		// Gán PlayerID: người chơi là RED, AI là BLACK
		redisMatchService.savePlayerId(matchEntity.getId(), playerId, true);
		redisMatchService.savePlayerId(matchEntity.getId(), aiId, false);

		redisMatchService.saveAiMode(matchEntity.getId(), mode);

		// Lượt đi đầu tiên là của người chơi
		redisMatchService.saveTurn(matchEntity.getId(), playerId);

		// Gán tên người chơi và AI
		redisMatchService.savePlayerName(matchEntity.getId(), player1.getUsername(), true);
		redisMatchService.savePlayerName(matchEntity.getId(), player2.getUsername(), false);

		// Gán rating của người chơi và AI
		redisMatchService.savePlayerRating(matchEntity.getId(), player1.getRating(), true);
		redisMatchService.savePlayerRating(matchEntity.getId(), player2.getRating(), false);

		// Gán thời gian mặc định cho cả hai bên
		redisMatchService.savePlayerTotalTimeLeft(matchEntity.getId(), PLAYER_TOTAL_TIME_LEFT, true);
		redisMatchService.savePlayerTotalTimeLeft(matchEntity.getId(), PLAYER_TOTAL_TIME_LEFT, false);
		// Gán trạng thái ready ban đầu
		redisMatchService.savePlayerReadyStatus(matchEntity.getId(), true, true);
		redisMatchService.savePlayerReadyStatus(matchEntity.getId(), false, true);

		// Initial player's turn time-expiration
		redisMatchService.savePlayerTurnTimeExpiration(matchEntity.getId(), PLAYER_TURN_TIME_EXPIRATION);

		return matchEntity.getId();
	}

	public MatchStateResponse getMatchStateById(Long matchId) {
		// Retrieve match state from Redis:
		// Get board state
		String boardStateJson = redisMatchService.getBoardStateJson(matchId);
		// Get player ID
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);
		// Get turn
		Long turn = redisMatchService.getTurn(matchId);
		// Get player's name
		String redPlayerName = redisMatchService.getPlayerName(matchId, true);
		String blackPlayerName = redisMatchService.getPlayerName(matchId, false);
		// Get player's rating
		Integer redPlayerRating = redisMatchService.getPlayerRating(matchId, true);
		Integer blackPlayerRating = redisMatchService.getPlayerRating(matchId, false);
		// Get player's timer-left
		Long redPlayerTimeLeft = redisMatchService.getPlayerTotalTimeLeft(matchId, true);
		Long blackPlayerTimeLeft = redisMatchService.getPlayerTotalTimeLeft(matchId, false);
		// Get last-move's time
		Instant lastMoveTime = redisMatchService.getLastMoveTime(matchId);

		// Return match state
		return MatchStateResponse.builder()
				.boardState(BoardUtils.boardParse(boardStateJson))
				.redPlayerId(redPlayerId)
				.blackPlayerId(blackPlayerId)
				.turn(turn)
				.redPlayerName(redPlayerName)
				.blackPlayerName(blackPlayerName)
				.redPlayerRating(redPlayerRating)
				.blackPlayerRating(blackPlayerRating)
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
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);
		// If this user isn't belong to this match
		if (!userId.equals(redPlayerId) && !userId.equals(blackPlayerId))
			throw new AppException(ErrorCode.MATCH_READY_INVALID);

		// Get user's faction
		boolean isRedPlayer = redPlayerId.equals(userId);

		// Update the ready status
		redisMatchService.savePlayerReadyStatus(matchId, isRedPlayer, true);

		Optional<String> roleOpt = playerRepository.findRoleById(blackPlayerId);
		if (roleOpt.isPresent() && "AI".equalsIgnoreCase(roleOpt.get())) {
			redisMatchService.savePlayerReadyStatus(matchId, false, true);
		}

		// Acquire lock
		redisMatchService.acquireMatchInitialLock(matchId);

		// Check to start match
		try {
			// Get opponent's ready-status
			Boolean opponentReadyStatus = redisMatchService.getPlayerReadyStatus(matchId, !isRedPlayer);
			// Get match's start-state through last-move time
			Instant lastMoveTime = redisMatchService.getLastMoveTime(matchId);

			// Start the match if the opponent is ready
			if (lastMoveTime == null && opponentReadyStatus) {
				// Initial last-move's time
				redisMatchService.saveLastMoveTime(matchId, Instant.now());
				// Get match state
				MatchStateResponse matchStateResponse = getMatchStateById(matchId);
				// Set Time Expiration
				redisMatchService.savePlayerTurnTimeExpiration(matchId, PLAYER_TURN_TIME_EXPIRATION);
				redisMatchService.savePlayerTotalTimeExpiration(matchId, PLAYER_TOTAL_TIME_EXPIRATION);

				// Notify both player: The match is start
				messagingTemplate.convertAndSend("/topic/match/player/" + redPlayerId,
						new ResponseObject("ok", "The match is start.", matchStateResponse));
				messagingTemplate.convertAndSend("/topic/match/player/" + blackPlayerId,
						new ResponseObject("ok", "The match is start.", matchStateResponse));
			}
		} finally {
			redisMatchService.releaseMatchInitialLock(matchId);
		}
	}

	public void move(Long matchId, MoveRequest moveRequest) {
		// Retrieve board state from Redis
		String boardStateJson = redisMatchService.getBoardStateJson(matchId);

		// Retrive match state from Redis
		String[][] boardState = BoardUtils.boardParse(boardStateJson);
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);
		Long turn = redisMatchService.getTurn(matchId);
		Long redPlayerTimeLeft = redisMatchService.getPlayerTotalTimeLeft(matchId, true);
		Long blackPlayerTimeLeft = redisMatchService.getPlayerTotalTimeLeft(matchId, false);
		Instant lastMoveTime = redisMatchService.getLastMoveTime(matchId);

		// Move validate
		boolean isValid = MoveValidator.isValidMove(
				boardState,
				moveRequest.getFrom().getRow(),
				moveRequest.getFrom().getCol(),
				moveRequest.getTo().getRow(),
				moveRequest.getTo().getCol()
		);

		if (!isValid) {
			throw new AppException(ErrorCode.INVALID_MOVE);
		}

		// Apply move & update Redis
		applyMove(matchId, redPlayerId, blackPlayerId, turn, boardState, moveRequest, redPlayerTimeLeft, blackPlayerTimeLeft, lastMoveTime);

		// Check if opponent has legal moves
		String opponentColor = redPlayerId.equals(turn) ? "black" : "red";
		if (!MoveValidator.hasLegalMoves(boardState, opponentColor))
			endMatch(matchId, redPlayerId.equals(turn) ? blackPlayerId : redPlayerId);
	}

	public void moveAI(Long matchId, MoveRequest moveRequest, boolean imAI) {
		// Retrieve board state from Redis
		String boardStateJson = redisMatchService.getBoardStateJson(matchId);

		// Retrive match state from Redis
		String[][] boardState = BoardUtils.boardParse(boardStateJson);
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);
		Long turn = redisMatchService.getTurn(matchId);
		Long redPlayerTimeLeft = redisMatchService.getPlayerTotalTimeLeft(matchId, true);
		Long blackPlayerTimeLeft = redisMatchService.getPlayerTotalTimeLeft(matchId, false);
		Instant lastMoveTime = redisMatchService.getLastMoveTime(matchId);

		// Get piece
		String piece = boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()];

		if (!imAI) {
			// Check if the piece belongs to the current player
			if (!isCorrectTurn(piece, redPlayerId, blackPlayerId, turn)) {
				throw new AppException(ErrorCode.INVALID_MOVE);
			}
		}

		// Move validate
		boolean isValid = MoveValidator.isValidMove(
				boardState,
				moveRequest.getFrom().getRow(),
				moveRequest.getFrom().getCol(),
				moveRequest.getTo().getRow(),
				moveRequest.getTo().getCol()
		);

		if (!isValid) {
			throw new AppException(ErrorCode.INVALID_MOVE);
		}

		// Apply move & update Redis
		applyMove(matchId, redPlayerId, blackPlayerId, turn, boardState, moveRequest, redPlayerTimeLeft, blackPlayerTimeLeft, lastMoveTime);

		// Check if opponent has legal moves
		String opponentColor = redPlayerId.equals(turn) ? "black" : "red";
		if (!MoveValidator.hasLegalMoves(boardState, opponentColor)) {
			endMatch(matchId, redPlayerId.equals(turn) ? blackPlayerId : redPlayerId);
			return;
		}

		if (!imAI) {
			String aiMode = redisMatchService.getAiMode(matchId);
			if (aiMode != null && !aiMode.isEmpty()) {
				MoveRequest moveAi = callAIMove(boardState, aiMode);
				moveAI(matchId, moveAi, true); // Thực hiện nước đi nếu hợp lệ
			}
		}
	}

	private static boolean isCorrectTurn(String piece, Long redPlayerId, Long blackPlayerId, Long turn) {
		// Get Jwt token from Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		// Get userId from token
		Long userId = jwt.getClaim("uid");

		return (Character.isUpperCase(piece.charAt(0)) && userId.equals(redPlayerId) && userId.equals(turn)) ||
				(Character.isLowerCase(piece.charAt(0)) && userId.equals(blackPlayerId) && userId.equals(turn));
	}

	public void resign(Long matchId) {
		// Get Jwt token from Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		// Get userId from token
		Long userId = jwt.getClaim("uid");

		// Get player ID
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);

		if (!userId.equals(redPlayerId) && !userId.equals(blackPlayerId))
			throw new AppException(ErrorCode.MATCH_READY_INVALID);

		endMatch(matchId, userId.equals(redPlayerId) ? redPlayerId : blackPlayerId);
	}

	private void applyMove(Long matchId, Long redPlayerId, Long blackPlayerId, Long currentTurn, String[][] boardState, MoveRequest moveRequest, Long redPlayerTimeLeft, Long blackPlayerTimeLeft, Instant lastMoveTime) {
		// Apply the move (update board state)
		boardState[moveRequest.getTo().getRow()][moveRequest.getTo().getCol()] =
				boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()]; // Move piece
		boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()] = ""; // Clear old position

		// Convert back to JSON and save to Redis
		String updatedBoardStateJson = BoardUtils.boardSerialize(boardState);
		redisMatchService.saveBoardStateJson(matchId, updatedBoardStateJson);

		// Get user's faction
		boolean isRedPlayer = currentTurn.equals(redPlayerId);

		// Switch turns
		Long nextTurn = isRedPlayer ? blackPlayerId : redPlayerId;
		redisMatchService.saveTurn(matchId, nextTurn);

		// Get current player's total time-left
		Long currentPlayerTimeLeft = isRedPlayer ? redPlayerTimeLeft : blackPlayerTimeLeft;
		// Update player's time-left
		redisMatchService.savePlayerTotalTimeLeft(matchId,
				currentPlayerTimeLeft - (Instant.now().toEpochMilli()-lastMoveTime.toEpochMilli()), isRedPlayer);

		// Update Last Move Time
		redisMatchService.saveLastMoveTime(matchId, Instant.now());

		// Get opponent player's total time-left
		Long opponentPlayerTimeLeft = isRedPlayer ? blackPlayerTimeLeft : redPlayerTimeLeft;

		// Set Time Expiration
		redisMatchService.savePlayerTurnTimeExpiration(matchId, PLAYER_TURN_TIME_EXPIRATION);
		redisMatchService.savePlayerTotalTimeExpiration(matchId, opponentPlayerTimeLeft);

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/match/player/" + blackPlayerId,
				new ResponseObject("ok", "Piece moved.", new MoveResponse(moveRequest.getFrom(), moveRequest.getTo())));
		messagingTemplate.convertAndSend("/topic/match/player/" + redPlayerId,
				new ResponseObject("ok", "Piece moved.", new MoveResponse(moveRequest.getFrom(), moveRequest.getTo())));
	}


	private MoveRequest callAIMove(String[][] boardState, String aiMode) {
		try {
			String url = switch (aiMode) {
                case "AI_EASY" -> "http://127.0.0.1:8000/next-move";
                case "AI_HARD" -> "http://127.0.0.1:8000/next-move-pikafish";
                default -> throw new IllegalArgumentException("Unknown AI mode: " + aiMode);
            };
            RestTemplate restTemplate = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			// Gửi mảng boardState trực tiếp dưới dạng JSON
			Map<String, Object> boardWrapper = new HashMap<>();
			boardWrapper.put("board", boardState);  // Truyền thẳng mảng 2 chiều boardState

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(boardWrapper, headers);

			// Gọi API Python
			ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Map<String, Integer> moveMap = response.getBody();

				Position from = new Position(moveMap.get("from_row"), moveMap.get("from_col"));
				Position to = new Position(moveMap.get("to_row"), moveMap.get("to_col"));

				return new MoveRequest(from, to);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null; // hoặc throw nếu cần
	}

	public void handleTimeout(Long matchId) {
		// Get lastMoveTime if the game is start or not
		Instant lastMoveTime = redisMatchService.getLastMoveTime(matchId);

		if (lastMoveTime != null) {
			endMatch(matchId, redisMatchService.getTurn(matchId));
		} else {
			cancelMatch(matchId);
		}
	}

	private void endMatch(Long matchId, Long resignPlayerId) {
		// Get PlayerId
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);

		// Get player's faction
		boolean	isRed = resignPlayerId.equals(redPlayerId);

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
		blackPlayerEntity.setRating(isRed ? blackPlayerEntity.getRating() + 10 : blackPlayerEntity.getRating() - 10);
		playerRepository.save(blackPlayerEntity);

		// Delete match state
		redisMatchService.deleteBoardState(matchEntity.getId());
		redisMatchService.deletePlayerId(matchEntity.getId(), true);
		redisMatchService.deletePlayerId(matchEntity.getId(), false);
		redisMatchService.deleteTurn(matchEntity.getId());
		redisMatchService.deletePlayerName(matchEntity.getId(), true);
		redisMatchService.deletePlayerName(matchEntity.getId(), false);
		redisMatchService.deletePlayerRating(matchEntity.getId(), true);
		redisMatchService.deletePlayerRating(matchEntity.getId(), false);
		redisMatchService.deleteTotalPlayerTimeLeft(matchEntity.getId(), true);
		redisMatchService.deleteTotalPlayerTimeLeft(matchEntity.getId(), false);
		redisMatchService.deleteLastMoveTime(matchEntity.getId());
		redisMatchService.deletePlayerReadyStatus(matchEntity.getId(), true);
		redisMatchService.deletePlayerReadyStatus(matchEntity.getId(), false);
		redisMatchService.deletePlayerTurnTimeExpiration(matchEntity.getId());
		redisMatchService.deletePlayerTotalTimeExpiration(matchEntity.getId());

		messagingTemplate.convertAndSend("/topic/match/player/" + (isRed ? blackPlayerId : redPlayerId),
				new ResponseObject("ok", "Match finished.", new MatchResultResponse("WIN", +10)));
		messagingTemplate.convertAndSend("/topic/match/player/" + (isRed ? redPlayerId : blackPlayerId),
				new ResponseObject("ok", "Match finished.", new MatchResultResponse("LOSE", -10)));
	}

	private void cancelMatch(Long matchId) {
		// Get PlayerId
		Long redPlayerId = redisMatchService.getPlayerId(matchId, true);
		Long blackPlayerId = redisMatchService.getPlayerId(matchId, false);

		// Update match info
		MatchEntity matchEntity = matchRepository.findById(matchId)
				.orElseThrow(() -> new AppException(ErrorCode.MATCH_NOT_FOUND));
		matchEntity.setResult("Match cancel."); // Opponent wins
		matchEntity.setEndTime(Instant.now());
		matchRepository.save(matchEntity);

		// Delete match state
		redisMatchService.deleteBoardState(matchEntity.getId());
		redisMatchService.deletePlayerId(matchEntity.getId(), true);
		redisMatchService.deletePlayerId(matchEntity.getId(), false);
		redisMatchService.deleteTurn(matchEntity.getId());
		redisMatchService.deletePlayerName(matchEntity.getId(), true);
		redisMatchService.deletePlayerName(matchEntity.getId(), false);
		redisMatchService.deletePlayerRating(matchEntity.getId(), true);
		redisMatchService.deletePlayerRating(matchEntity.getId(), false);
		redisMatchService.deleteTotalPlayerTimeLeft(matchEntity.getId(), true);
		redisMatchService.deleteTotalPlayerTimeLeft(matchEntity.getId(), false);
		redisMatchService.deleteLastMoveTime(matchEntity.getId());
		redisMatchService.deletePlayerReadyStatus(matchEntity.getId(), true);
		redisMatchService.deletePlayerReadyStatus(matchEntity.getId(), false);
		redisMatchService.deletePlayerTurnTimeExpiration(matchEntity.getId());
		redisMatchService.deletePlayerTotalTimeExpiration(matchEntity.getId());

		messagingTemplate.convertAndSend("/topic/match/player/" + blackPlayerId,
				new ResponseObject("ok", "Match cancel.", new MatchResultResponse("CANCEL", null)));
		messagingTemplate.convertAndSend("/topic/match/player/" + redPlayerId,
				new ResponseObject("ok", "Match cancel.", new MatchResultResponse("CANCEL", null)));
	}
}
