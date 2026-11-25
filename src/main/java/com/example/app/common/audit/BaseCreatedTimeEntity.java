package com.example.app.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 생성 시각만 기록하는 Entity의 상위 클래스. 한 번 저장하면 바뀌지 않는 기록(이력, 로그성 데이터)에 쓴다.
 *
 * <p>값을 바꿀 수 있는 Entity는 {@link BaseTimeEntity}를 쓴다. 시각은 {@code JpaAuditingConfig}가 {@code Clock} 빈으로
 * 정한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
