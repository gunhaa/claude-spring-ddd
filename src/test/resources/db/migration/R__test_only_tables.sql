-- 테스트 전용 Entity의 테이블. src/test/resources에 있으므로 테스트 클래스패스에서만 적용되고 운영 jar에는 들어가지 않는다.
-- 버전 없는 repeatable 마이그레이션(R__)이라 src/main의 V{n} 번호와 겹치지 않는다.

-- common.audit.JpaAuditingTest
CREATE TABLE IF NOT EXISTS test_time_audited_entity (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS test_created_time_audited_entity (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255),
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);
