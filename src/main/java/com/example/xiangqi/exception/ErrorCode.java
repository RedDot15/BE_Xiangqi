package com.example.xiangqi.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    // General
    UNCATEGORIZED(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error."),
    // Player
    USER_DUPLICATE(HttpStatus.CONFLICT, "User already exists."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "Wrong password."),
    // Authentication
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Unauthenticated error."),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "You do not have permission to perform this operation."),
    // Invite
    OPPONENT_STATUS_IN_MATCH(HttpStatus.BAD_REQUEST, "This player is in match."),
    OPPONENT_STATUS_QUEUE(HttpStatus.BAD_REQUEST, "This player is in queue."),
    INVITATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "This invitation already exists."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "This invitation does not exist."),
    // Queue
    EMPTY_QUEUE(HttpStatus.CONFLICT, "Queue is empty."),
    // Match contract
    MATCH_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "Match contract not found."),
    // Match
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Match not found."),
    // Board State
    BOARD_STATE_SERIALIZED_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize board state"),
    BOARD_STATE_PARSING_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "Error parsing board state from Redis"),
    // Move
    INVALID_MOVE(HttpStatus.BAD_REQUEST, "Invalid move."),
    ;

    HttpStatus httpStatus;
    String message;
}
