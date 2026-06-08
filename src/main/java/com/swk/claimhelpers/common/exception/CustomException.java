package com.swk.claimhelpers.common.exception;

import lombok.Getter;

/**
 * 도메인 서비스에서의 사용 예:
 *   throw new CustomException(ErrorCode.FORBIDDEN);                       // 기본 메시지 사용
 *   throw new CustomException(ErrorCode.FORBIDDEN, "다른 사용자의 약관입니다."); // 메시지 override
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    // 기본 메시지 사용. 예외 메시지(super)에도 ErrorCode 의 기본 메시지를 그대로 넣어 로그 추적을 돕는다.
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 메시지 override. 상황에 맞는 구체적 메시지를 클라이언트에 내려주고 싶을 때 사용한다.
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}