package com.example.xiangqi.exception;

import com.example.xiangqi.dto.response.ValidationResponse;
import com.example.xiangqi.helper.ResponseObject;
import jakarta.validation.ConstraintViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public static final String FIELDS_ATTRIBUTE = "fields";
    public static final String MIN_ATTRIBUTE = "min";
    public static final String MAX_ATTRIBUTE = "max";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseObject> handleGeneralException(Exception e) {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED;
        return buildResponse(errorCode.getHttpStatus(), errorCode.getMessage(), null);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ResponseObject> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        return buildResponse(errorCode.getHttpStatus(), errorCode.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseObject> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return buildResponse(errorCode.getHttpStatus(), errorCode.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseObject> handleValidationException(MethodArgumentNotValidException ex) {
        List<ValidationResponse> validationResponseSet = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    Map<String, Object> attributes = error.unwrap(ConstraintViolation.class)
                            .getConstraintDescriptor()
                            .getAttributes();
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        return new ValidationResponse(
                                fieldError.getField(),
                                Objects.nonNull(attributes)
                                        ? mapAttribute(
                                        Objects.requireNonNull(fieldError.getDefaultMessage()), attributes)
                                        : fieldError.getDefaultMessage());
                    } else { // Handle ObjectError (class-level constraints)
                        String[] fields = (String[]) attributes.get(FIELDS_ATTRIBUTE);
                        return new ValidationResponse(
                                Arrays.asList(fields),
                                Objects.nonNull(attributes)
                                        ? mapAttribute(Objects.requireNonNull(error.getDefaultMessage()), attributes)
                                        : error.getDefaultMessage());
                    }
                })
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed.", validationResponseSet);
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        String maxValue = String.valueOf(attributes.get(MAX_ATTRIBUTE));
        String[] fields = (String[]) attributes.get(FIELDS_ATTRIBUTE);

        message = message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
        message = message.replace("{" + MAX_ATTRIBUTE + "}", maxValue);
        if (Objects.nonNull(fields))
            message = message.replace("{" + FIELDS_ATTRIBUTE + "}", "[" + String.join(", ", fields) + "]");
        return message;
    }
}
