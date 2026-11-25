package com.example.app.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    @DisplayName("상세 메시지를 주지 않으면 오류 코드의 기본 메시지를 쓴다")
    void constructor_withoutDetail_usesDefaultMessage() {
        BusinessException ex = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND.message());
    }

    @Test
    @DisplayName("상세 메시지를 주면 그 메시지를 쓴다")
    void constructor_withDetail_usesDetail() {
        BusinessException ex = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "상품 1을 찾을 수 없습니다.");

        assertThat(ex.getMessage()).isEqualTo("상품 1을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("공통 오류 코드의 code는 enum 상수 이름이다")
    void commonErrorCode_code_isEnumName() {
        assertThat(CommonErrorCode.INVALID_INPUT.code()).isEqualTo("INVALID_INPUT");
    }
}
