package com.example.xiangqi.service.my_sql;

import com.example.xiangqi.dto.request.MoveRequest;
import com.example.xiangqi.dto.request.ResignRequest;
import com.example.xiangqi.dto.response.*;
import com.example.xiangqi.entity.my_sql.MatchEntity;
import com.example.xiangqi.entity.my_sql.PlayerEntity;
import com.example.xiangqi.entity.redis.MatchStateEntity;
import com.example.xiangqi.entity.redis.MatchStatePlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.dto.response.PageResponse;
import com.example.xiangqi.helper.MessageObject;
import com.example.xiangqi.mapper.MatchMapper;
import com.example.xiangqi.mapper.MatchStateMapper;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
	private final MatchStateMapper matchStateMapper;

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

	public Long createMatch(Long player1Id, Long player2Id, boolean isRank) {
		MatchEntity matchEntity = new MatchEntity();

		// Find the two players
		PlayerEntity player1 = playerRepository.findById(player1Id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		PlayerEntity player2 = playerRepository.findById(player2Id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		// Randomly assign colors
		boolean firstIsRed = Math.random() < 0.5;
		matchEntity.setRedPlayerEntity(firstIsRed ? player1 : player2);
		matchEntity.setBlackPlayerEntity(firstIsRed ? player2: player1);

		// Save match
		matchRepository.save(matchEntity);

		// Initial match state
		redisMatchService.saveMatchState(
				matchEntity.getId(),
				MatchStateEntity.builder()
						.boardState(BoardUtils.getInitialBoardState())
						.redPlayer(MatchStatePlayerEntity.builder()
								.id(firstIsRed ? player1.getId() : player2.getId())
								.name(firstIsRed ? player1.getUsername() : player2.getUsername())
								.rating(firstIsRed ? player1.getRating() : player2.getRating())
								.totalTimeLeft(PLAYER_TOTAL_TIME_LEFT)
								.build())
						.blackPlayer(MatchStatePlayerEntity.builder()
								.id(firstIsRed ? player2.getId() : player1.getId())
								.name(firstIsRed ? player2.getUsername() : player1.getUsername())
								.rating(firstIsRed ? player2.getRating() : player1.getRating())
								.totalTimeLeft(PLAYER_TOTAL_TIME_LEFT)
								.build())
						.turn(firstIsRed ? player1.getId() : player2.getId())
						.lastMoveTime(Instant.now())
						.mode(isRank ? "RANK" : "NORMAL")
						.build());
		// Initial match expiration
		redisMatchService.saveMatchExpiration(matchEntity.getId(),
				Math.min(PLAYER_TURN_TIME_EXPIRATION, PLAYER_TOTAL_TIME_EXPIRATION));

		return matchEntity.getId();
	}

	public MatchStateResponse getMatchStateById(Long matchId) {
		// Get match state
		MatchStateEntity entity = redisMatchService.getMatchState(matchId);
		// Mapping & Return match state
		return matchStateMapper.toResponse(entity);
	}

	public void move(MoveRequest moveRequest) {
		// Get match state
		MatchStateEntity msEntity = redisMatchService.getMatchState(moveRequest.getMatchId());

		// Check if the piece belongs to the current player
		if (!isCorrectTurn(msEntity, moveRequest))
			throw new AppException(ErrorCode.UNAUTHORIZED);
		// Move validate
		if (!MoveValidator.isValidMove(msEntity.getBoardState(), moveRequest))
			throw new AppException(ErrorCode.INVALID_MOVE);

		// Apply move & update Redis
		applyMove(moveRequest.getMatchId(), msEntity, moveRequest);

		// Notify players via WebSocket
		messagingTemplate.convertAndSend("/topic/match/" + moveRequest.getMatchId(),
				new MessageObject("Piece moved.", new MoveResponse(moveRequest.getFrom(), moveRequest.getTo())));

		// Check if opponent has legal moves
		boolean opponentIsRed = msEntity.getRedPlayer().getId().equals(msEntity.getTurn());
		if (!MoveValidator.hasLegalMoves(msEntity.getBoardState(), opponentIsRed))
			endMatch(moveRequest.getMatchId(), opponentIsRed
					? msEntity.getRedPlayer().getId()
					: msEntity.getBlackPlayer().getId());
	}

	private static boolean isCorrectTurn(MatchStateEntity msEntity, MoveRequest request) {
		// Get match state
		Long redPlayerId = msEntity.getRedPlayer().getId();
		Long blackPlayerId = msEntity.getBlackPlayer().getId();
		Long turn = msEntity.getTurn();
		// Get moved piece
		String movedPiece = msEntity.getBoardState()[request.getFrom().getRow()][request.getFrom().getCol()];

		return (Character.isUpperCase(movedPiece.charAt(0)) && request.getMoverId().equals(redPlayerId) && request.getMoverId().equals(turn)) ||
				(Character.isLowerCase(movedPiece.charAt(0)) && request.getMoverId().equals(blackPlayerId) && request.getMoverId().equals(turn));
	}

	public void resign(ResignRequest resignRequest) {
		// Get player ID
		MatchStateEntity msEntity = redisMatchService.getMatchState(resignRequest.getMatchId());
		Long redPlayerId = msEntity.getRedPlayer().getId();
		Long blackPlayerId = msEntity.getBlackPlayer().getId();

		if (!resignRequest.getSurrenderId().equals(redPlayerId) && !resignRequest.getSurrenderId().equals(blackPlayerId))
			throw new AppException(ErrorCode.UNAUTHORIZED);

		endMatch(resignRequest.getMatchId(), resignRequest.getSurrenderId().equals(redPlayerId) ? redPlayerId : blackPlayerId);
	}

	private void applyMove(Long matchId, MatchStateEntity msEntity, MoveRequest moveRequest) {
		// Get match state
		String[][] boardState = msEntity.getBoardState();
		MatchStatePlayerEntity redPlayer = msEntity.getRedPlayer();
		MatchStatePlayerEntity blackPlayer = msEntity.getBlackPlayer();
		Instant lastMoveTime = msEntity.getLastMoveTime();
		// Get move request detail
		int fromRow = moveRequest.getFrom().getRow();
		int fromCol = moveRequest.getFrom().getCol();
		int toRow = moveRequest.getTo().getRow();
		int toCol = moveRequest.getTo().getCol();
		// Get user's faction
		boolean isRedPlayer = msEntity.getTurn().equals(redPlayer.getId());

		// Apply the move (update board state)
		boardState[toRow][toCol] = boardState[fromRow][fromCol]; // Move piece
		boardState[fromRow][fromCol] = ""; // Clear old position
		// Update turns
		Long nextTurn = isRedPlayer ? blackPlayer.getId() : redPlayer.getId();
		msEntity.setTurn(nextTurn);
		// Get current player's total time-left
		Long currentPlayerTimeLeft = isRedPlayer ? redPlayer.getTotalTimeLeft() : blackPlayer.getTotalTimeLeft();
		// Calculate player's total time-left
		Long updatedPlayerTimeLeft = currentPlayerTimeLeft -
				(Instant.now().toEpochMilli()-lastMoveTime.toEpochMilli());
		// Update player's time-left
		if (isRedPlayer)
			redPlayer.setTotalTimeLeft(updatedPlayerTimeLeft);
		else
			blackPlayer.setTotalTimeLeft(updatedPlayerTimeLeft);
		// Update Last Move Time
		msEntity.setLastMoveTime(Instant.now());

		// Update match state
		redisMatchService.saveMatchState(matchId, msEntity);

		// Get opponent player's total time-left
		Long opponentPlayerTimeLeft = isRedPlayer
				? blackPlayer.getTotalTimeLeft()
				: redPlayer.getTotalTimeLeft();

		// Set new match expiration
		redisMatchService.saveMatchExpiration(matchId, Math.min(PLAYER_TURN_TIME_EXPIRATION, opponentPlayerTimeLeft));
	}

	public void handleMatchExpiration(Long matchId) {
		// Get match state
		MatchStateEntity entity = redisMatchService.getMatchState(matchId);
		// End match
		endMatch(matchId, entity.getTurn());
	}

	private void endMatch(Long matchId, Long loserId) {
		// Get match state
		MatchStateEntity msEntity = redisMatchService.getMatchState(matchId);
		// Get PlayerId
		Long redPlayerId = msEntity.getRedPlayer().getId();
		Long blackPlayerId = msEntity.getBlackPlayer().getId();

		// Get player's faction
		boolean	isRedLose = loserId.equals(redPlayerId);

		// Update match info
		MatchEntity matchEntity = matchRepository.findById(matchId)
				.orElseThrow(() -> new AppException(ErrorCode.MATCH_NOT_FOUND));
		matchEntity.setResult(isRedLose ? "Black Player Win" : "Red Player Win"); // Opponent wins
		matchEntity.setEndTime(Instant.now());
		matchRepository.save(matchEntity);

		// Get mode
		boolean isRank = msEntity.getMode().equals("RANK");

		if (isRank) {
			// Update red's elo
			PlayerEntity redPlayerEntity = playerRepository.findById(redPlayerId)
					.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
			redPlayerEntity.setRating(isRedLose ? redPlayerEntity.getRating() - 10 : redPlayerEntity.getRating() + 10);
			playerRepository.save(redPlayerEntity);
			// Update black's elo
			PlayerEntity blackPlayerEntity = playerRepository.findById(blackPlayerId)
					.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
			blackPlayerEntity.setRating(isRedLose ? blackPlayerEntity.getRating() + 10 : blackPlayerEntity.getRating() - 10);
			playerRepository.save(blackPlayerEntity);
		}

		// Delete match state
		redisMatchService.deleteMatchState(matchEntity.getId());
		redisMatchService.deleteMatchExpiration(matchEntity.getId());

		messagingTemplate.convertAndSend("/topic/match/" + matchId,
				new MessageObject(
						"Match finished.",
						new MatchResultResponse(
								isRedLose ? "black" : "red",
								isRank ? +10 : 0,
								isRank ? -10 : 0)));
	}
}
