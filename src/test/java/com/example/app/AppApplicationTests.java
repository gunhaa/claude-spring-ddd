package com.example.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.common.web.TraceIdFilter;
import com.example.app.support.TestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class AppApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("애플리케이션 컨텍스트가 뜬다")
    void contextLoads() {}

    @Test
    @DisplayName("테스트는 test 프로필로 뜨고 로컬 .env.example·.env를 읽지 않는다")
    void testProfile_doesNotReadLocalEnvFiles() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getDefaultProfiles()).containsExactly("test");
        assertThat(environment.getProperty("DB_HOST")).isNull();
    }

    @Test
    @DisplayName("로그는 ECS JSON 한 줄로 출력되고 MDC의 추적 ID를 포함한다")
    void logging_usesEcsFormatWithTraceId(CapturedOutput output) {
        MDC.put(TraceIdFilter.MDC_KEY, "trace-ecs-check");
        try {
            LoggerFactory.getLogger(getClass()).info("ecs-check");
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }

        String line = output.getOut()
                .lines()
                .filter(l -> l.contains("ecs-check"))
                .findFirst()
                .orElseThrow();
        assertThat(line)
                .startsWith("{")
                .contains("\"ecs\":{\"version\"")
                .contains("\"trace\":{\"id\":\"trace-ecs-check\"}");
    }
}
