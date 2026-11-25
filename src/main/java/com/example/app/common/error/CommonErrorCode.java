package com.example.app.common.error;

/** 특정 Bounded Context에 속하지 않는 공통 오류 코드. */
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT(ErrorType.INVALID_INPUT, "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(ErrorType.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(ErrorType.INTERNAL, "서버 내부 오류가 발생했습니다.");

    private final ErrorType type;
    private final String message;

    CommonErrorCode(ErrorType type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String message() {
        return message;
    }
}
