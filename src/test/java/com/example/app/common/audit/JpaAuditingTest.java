package com.example.app.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.common.config.JpaAuditingConfig;
import com.example.app.support.TestcontainersConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// 테스트 전용 Entity의 테이블은 src/test/resources/db/migration/R__test_only_tables.sql이 만든다
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TestcontainersConfig.class, JpaAuditingConfig.class, JpaAuditingTest.TestClockConfig.class})
class JpaAuditingTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MutableClock clock;

    @Test
    @DisplayName("저장하면 생성 시각과 수정 시각을 Clock의 현재 시각으로 채운다")
    void persist_setsCreatedAtAndUpdatedAt() {
        clock.setInstant(START);

        TimeAuditedEntity entity = entityManager.persistAndFlush(new TimeAuditedEntity("처음"));

        LocalDateTime expected = LocalDateTime.ofInstant(START, ZONE);
        assertThat(entity.getCreatedAt()).isEqualTo(expected);
        assertThat(entity.getUpdatedAt()).isEqualTo(expected);
    }

    @Test
    @DisplayName("수정하면 수정 시각만 바뀌고 생성 시각은 그대로다")
    void update_changesOnlyUpdatedAt() {
        clock.setInstant(START);
        TimeAuditedEntity entity = entityManager.persistAndFlush(new TimeAuditedEntity("처음"));

        clock.setInstant(START.plus(Duration.ofHours(1)));
        entity.changeName("수정");
        entityManager.flush();

        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(START, ZONE));
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(START.plus(Duration.ofHours(1)), ZONE));
    }

    @Test
    @DisplayName("생성 시각만 기록하는 Entity는 저장할 때 생성 시각을 채운다")
    void persist_createdTimeOnly_setsCreatedAt() {
        clock.setInstant(START);

        CreatedTimeAuditedEntity entity = entityManager.persistAndFlush(new CreatedTimeAuditedEntity("기록"));

        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(START, ZONE));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        MutableClock clock() {
            return new MutableClock(START, ZONE);
        }
    }

    /** 테스트에서 현재 시각을 바꿀 수 있는 Clock. */
    static final class MutableClock extends Clock {

        private final ZoneId zone;
        private Instant instant;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant newInstant) {
            this.instant = newInstant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(instant, newZone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Entity
    @Table(name = "test_time_audited_entity")
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class TimeAuditedEntity extends BaseTimeEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        TimeAuditedEntity(String name) {
            this.name = name;
        }

        void changeName(String newName) {
            this.name = newName;
        }
    }

    @Entity
    @Table(name = "test_created_time_audited_entity")
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class CreatedTimeAuditedEntity extends BaseCreatedTimeEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        CreatedTimeAuditedEntity(String name) {
            this.name = name;
        }
    }
}
