package com.example.app.common.error;

/**
 * 비즈니스 규칙 위반을 나타내는 예외. 도메인·애플리케이션 계층은 이 예외(또는 하위 클래스)만 던지고, HTTP 응답 변환은
 * {@code GlobalExceptionHandler}가 맡는다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    /** @param detail 응답의 {@code detail}로 나가는 메시지. 클라이언트에 노출되므로 내부 정보를 넣지 않는다. */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
