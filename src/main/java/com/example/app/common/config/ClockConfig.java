package com.example.app.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각은 이 {@link Clock} 빈으로만 얻는다({@code LocalDateTime.now(clock)}). 테스트에서 고정 시계로 바꿀 수 있다.
 *
 * <p>시간대는 JVM 기본값이 아니라 {@code app.time-zone}(환경 변수 {@code APP_TIME_ZONE})으로 정한다. 실행 환경(로컬 KST, 컨테이너
 * UTC 등)에 따라 {@code LocalDateTime} 값의 기준이 달라지지 않게 하기 위해서다.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}
