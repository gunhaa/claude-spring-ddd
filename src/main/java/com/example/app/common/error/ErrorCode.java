package com.example.app.common.error;

/**
 * 응답의 {@code code} 필드로 나가는 오류 코드. Bounded Context마다 이 인터페이스를 구현한 enum을 둔다.
 *
 * <pre>{@code
 * public enum ProductErrorCode implements ErrorCode {
 *     PRODUCT_NOT_FOUND(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
 *     ...
 * }
 * }</pre>
 */
public interface ErrorCode {

    /** 클라이언트가 분기에 쓰는 고정 문자열. enum 상수 이름을 그대로 쓴다. */
    String code();

    ErrorType type();

    /** 상세 메시지를 따로 주지 않았을 때 쓰는 기본 메시지. */
    String message();
}
