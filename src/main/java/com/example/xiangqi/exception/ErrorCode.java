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
    USERNAME_DUPLICATE(HttpStatus.CONFLICT, "Username already exists."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    // Authentication
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Unauthenticated error."),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "You do not have permission to perform this operation."),
    // Queue
    EMPTY_QUEUE(HttpStatus.CONFLICT, "Queue is empty."),
    UNQUEUE_INVALID(HttpStatus.CONFLICT, "Unqueue is invalid."),
    ;

    HttpStatus httpStatus;
    String message;
}
