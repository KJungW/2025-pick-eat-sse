package com.pickeat.sse.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

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
