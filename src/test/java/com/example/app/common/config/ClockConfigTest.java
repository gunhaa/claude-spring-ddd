package com.example.app.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClockConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(ClockConfig.class);

    @Test
    @DisplayName("Clock은 JVM 기본 시간대가 아니라 app.time-zone으로 설정한 시간대를 쓴다")
    void clock_usesConfiguredTimeZone() {
        // given: JVM 기본 시간대와 다른 시간대를 골라, 기본 시간대로 되돌아가면 실패하게 한다
        ZoneId expected = ZoneId.systemDefault().equals(ZoneId.of("Pacific/Auckland"))
                ? ZoneId.of("America/Sao_Paulo")
                : ZoneId.of("Pacific/Auckland");

        // when / then
        contextRunner
                .withPropertyValues("app.time-zone=" + expected.getId())
                .run(context ->
                        assertThat(context.getBean(Clock.class).getZone()).isEqualTo(expected));
    }
}
