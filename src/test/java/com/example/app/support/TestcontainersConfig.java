package com.example.app.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * DB가 필요한 테스트에 MySQL 컨테이너를 붙인다. {@code @Import(TestcontainersConfig.class)}로 쓴다.
 *
 * <p>{@code @ServiceConnection}이 컨테이너 접속 정보로 {@code spring.datasource.*}를 덮어쓰므로 로컬 MySQL이나 개발 DB를 건드리지
 * 않는다. 컨테이너는 static 하나를 모든 테스트 컨텍스트(@SpringBootTest, @DataJpaTest 등)가 함께 쓴다. Docker가 필요하다.
 *
 * <p>{@code destroyMethod = ""}: Spring은 {@code AutoCloseable} 빈의 {@code close()}를 컨텍스트가 닫힐 때 자동으로 부른다.
 * 공유 컨테이너를 컨텍스트 하나가 닫히면서({@code @DirtiesContext}, 컨텍스트 캐시 축출) 멈추지 않도록 끄고, 정리는 JVM 종료 시
 * Testcontainers(Ryuk)에 맡긴다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /** compose.yaml과 같은 이미지·문자셋. */
    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    /** DB 시간대는 앱과 같은 APP_TIME_ZONE(test.env)이다. 컨테이너는 첫 컨텍스트에서 시작하고, 모든 컨텍스트가 같은 값을 읽는다. */
    @Bean(destroyMethod = "")
    @ServiceConnection
    MySQLContainer mysqlContainer(@Value("${app.time-zone}") String timeZone) {
        return MYSQL.withEnv("TZ", timeZone);
    }
}
