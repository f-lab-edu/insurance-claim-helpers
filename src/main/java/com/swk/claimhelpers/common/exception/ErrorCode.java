package com.swk.claimhelpers.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 공통 에러 코드.
 * 각 상수는 HTTP 상태(status)와 사용자에게 노출할 기본 메시지(message)를 함께 보유한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 약관(claim_criteria) 조회/삭제 에러
    CLAIM_CRITERIA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 약관을 찾을 수 없습니다."),
    CLAIM_CRITERIA_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "약관 처리가 아직 완료되지 않았습니다."),

    // 채팅 세션 조회 에러
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 채팅 세션을 찾을 수 없습니다."),
    NO_CLAIM_CRITERIA_ATTACHED(HttpStatus.BAD_REQUEST, "세션에 연결된 약관이 없습니다."),

    // 파일 업로드 요청 검증 에러
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기 제한을 초과했습니다."),

    // 파일 스토리지(S3) 관련 에러
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 다운로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");

    private final HttpStatus status;

    // 클라이언트에 노출할 기본 메시지. CustomException 에서 override 하지 않으면 이 값이 사용된다.
    private final String message;
}