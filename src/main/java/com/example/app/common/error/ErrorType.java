package com.example.app.common.error;

/**
 * 오류의 종류. 도메인은 HTTP를 모르므로 의미만 표현하고, HTTP 상태 코드로의 변환은 웹 계층의
 * {@code GlobalExceptionHandler}가 맡는다.
 */
public enum ErrorType {
    INVALID_INPUT,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL
}
