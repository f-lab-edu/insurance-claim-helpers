package com.swk.claimhelpers.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 처리 대상:
 *   - CustomException                 : ErrorCode 에 정의된 status/message
 *   - MethodArgumentNotValidException : @Valid 검증 실패 -> 400 INVALID_INPUT
 *   - Exception (fallback)            : 그 외 모든 예외 -> 500 INTERNAL_ERROR
 *
 * 참고: 인증되지 않은 요청(401)/인가 실패(403)는 Spring Security 의
 * AuthenticationEntryPoint / AccessDeniedHandler 가 이 advice 보다 먼저 가로채므로
 * 여기서는 다루지 않는다. (SecurityConfig 소관 — #11 머지 후 별도 통일 예정)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse body = new ErrorResponse(errorCode.name(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    /**
     * @Valid / @Validated 요청 바디 검증 실패 처리.
     * 어떤 필드가 왜 실패했는지는 로그로만 남기고, 클라이언트에는 일반화된 INVALID_INPUT 메시지를 내려준다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("요청 값 검증 실패: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}