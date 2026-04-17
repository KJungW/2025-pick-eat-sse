package com.pickeat.sse.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 인증 관련 에러,
    HEADER_IS_EMPTY(HttpStatus.UNAUTHORIZED, "인증 헤더가 존재하지 않습니다."),
    TOKEN_IS_EMPTY(HttpStatus.FORBIDDEN, "인증되지 않은 사용자입니다."),
    INVALID_TOKEN(HttpStatus.FORBIDDEN, "잘못된 인증 정보입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    //입력 검증 에러
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력 데이터 검증에 실패했습니다."),

    // 시스템 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
