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
import com.example.xiangqi.repository.UserRepository;
import com.example.xiangqi.util.BoardUtils;
import com.example.xiangqi.util.MoveValidator;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class MatchService {
	MatchRepository matchRepository;
	SimpMessagingTemplate messagingTemplate; // For WebSocket notifications
	PlayerRepository playerRepository;
	RedisService redisService;

	private static final String MATCH_SUCCESS = "MATCH_FOUND";
	private final UserRepository userRepository;

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

		// Initialize board state & store in Redis
		String initialBoard = BoardUtils.getInitialBoardState();
		redisService.saveBoardStateJson(matchEntity.getId(), initialBoard);
		redisService.savePlayerId(matchEntity.getId(), firstIsRed ? player1Id : player2Id, true);
		redisService.savePlayerId(matchEntity.getId(), firstIsRed ? player2Id : player1Id, false);
		redisService.saveTurn(matchEntity.getId(), firstIsRed ? player1Id : player2Id);

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/queue/" + player1Id,
				new ResponseObject("ok", "Match found.", new QueueResponse(matchEntity.getId(), MATCH_SUCCESS)));
		messagingTemplate.convertAndSend("/topic/queue/" + player2Id,
				new ResponseObject("ok", "Match found.", new QueueResponse(matchEntity.getId(), MATCH_SUCCESS)));

		return matchEntity.getId();
	}

	public MatchStateResponse getMatchStateById(Long matchId) {
		// Retrieve match state from Redis
		String boardStateJson = redisService.getBoardStateJson(matchId);
		Long redPlayerId = redisService.getPlayerId(matchId, true);
		Long blackPlayerId = redisService.getPlayerId(matchId, false);
		Long turn = redisService.getTurn(matchId);

		// Return match state
		return MatchStateResponse.builder()
				 .boardState(BoardUtils.boardParse(boardStateJson))
				 .redPlayerId(redPlayerId)
				 .blackPlayerId(blackPlayerId)
				 .turn(turn)
				 .build();
	}

	public MoveResponse move(Long matchId, MoveRequest moveRequest) {
		// Retrieve board state from Redis
		String boardStateJson = redisService.getBoardStateJson(matchId);

		// Retrive match state from Redis
		String[][] boardState = BoardUtils.boardParse(boardStateJson);
		Long redPlayerId = redisService.getPlayerId(matchId, true);
		Long blackPlayerId = redisService.getPlayerId(matchId, false);
		Long turn = redisService.getTurn(matchId);

		// Move validate
		if (!MoveValidator.isValidMove(boardState, redPlayerId, blackPlayerId, turn,
				moveRequest.getFrom().getRow(), moveRequest.getFrom().getCol(), moveRequest.getTo().getRow(),
				moveRequest.getTo().getCol())) {
			throw new AppException(ErrorCode.INVALID_MOVE);
		}

		// Apply move & update Redis
		applyMove(matchId, redPlayerId, blackPlayerId, turn, boardState, moveRequest);

		return MoveResponse.builder()
				.from(moveRequest.getFrom())
				.to(moveRequest.getTo())
				.build();
	}

	public MatchResultResponse resign(Long matchId) {
		// Get Jwt token from Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) authentication.getPrincipal();
		// Get userId from token
		Long userId = jwt.getClaim("uid");

		// Get PlayerId
		Long redPlayerId = redisService.getPlayerId(matchId, true);
		Long blackPlayerId = redisService.getPlayerId(matchId, false);
		// Get current turn
		Long turn = redisService.getTurn(matchId);

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
		redisService.deleteBoardState(matchEntity.getId());
		redisService.deletePlayerId(matchEntity.getId(), true);
		redisService.deletePlayerId(matchEntity.getId(), false);
		redisService.deleteTurn(matchEntity.getId());

		messagingTemplate.convertAndSend("/topic/match/" + (isRed ? blackPlayerId : redPlayerId),
				new ResponseObject("ok", "You win the match", new MatchResultResponse("WIN", isRed ? blackPlayerEntity.getRating() : redPlayerEntity.getRating())));

		return new MatchResultResponse("LOSE", isRed ? redPlayerEntity.getRating() : blackPlayerEntity.getRating());
	}

	private void applyMove(Long matchId, Long redPlayerId, Long blackPlayerId, Long currentTurn, String[][] boardState, MoveRequest moveRequest) {
		// Apply the move (update board state)
		boardState[moveRequest.getTo().getRow()][moveRequest.getTo().getCol()] =
				boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()]; // Move piece
		boardState[moveRequest.getFrom().getRow()][moveRequest.getFrom().getCol()] = ""; // Clear old position

		// Convert back to JSON and save to Redis
		String updatedBoardStateJson = BoardUtils.boardSerialize(boardState);
		redisService.saveBoardStateJson(matchId, updatedBoardStateJson);

		// Switch turns
		Long nextTurn = currentTurn.equals(redPlayerId) ? blackPlayerId : redPlayerId;
		redisService.saveTurn(matchId, nextTurn);

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/match/" + (currentTurn.equals(redPlayerId) ? blackPlayerId : redPlayerId),
				new ResponseObject("ok", "Opponent player has moved.", new MoveResponse(moveRequest.getFrom(), moveRequest.getTo())));
	}

}
