package com.example.app.common.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing({@code @CreatedDate}, {@code @LastModifiedDate})을 켠다.
 *
 * <p>메인 클래스가 아니라 별도 설정으로 두어 {@code @WebMvcTest} 같은 웹 슬라이스가 JPA 설정을 읽지 않게 한다. 반대로
 * {@code @DataJpaTest}는 {@code @Configuration}을 스캔하지 않으므로 이 클래스를 {@code @Import} 해야 Auditing이 동작한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
