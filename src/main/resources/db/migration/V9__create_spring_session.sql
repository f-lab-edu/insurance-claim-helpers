-- Spring Session JDBC 저장소용 스키마 (spring-session-jdbc 공식 PostgreSQL 스크립트 기준).
--
-- 역할: HttpSession을 JVM 메모리가 아닌 DB에 저장 → 서버 재시작/다중 서버에도 세션 유지.
-- 소유: Spring Session 프레임워크가 관리하는 인프라 테이블.
-- 자동 초기화: spring.session.jdbc.initialize-schema=never 로 끄고 스키마는 Flyway가 관리한다.
-- PRINCIPAL_NAME(로그인) / SESSION_ID(=session_key, 비로그인) 값으로만 논리 연결한다.

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,  -- 내부 PK (UUID 문자열)
    SESSION_ID            CHAR(36)     NOT NULL,  -- 클라이언트 쿠키에 실리는 세션 ID (재발급 시 변경되어 PK와 분리)
    CREATION_TIME         BIGINT       NOT NULL,  -- 생성 시각 (epoch millis)
    LAST_ACCESS_TIME      BIGINT       NOT NULL,  -- 마지막 접근 시각 (epoch millis)
    MAX_INACTIVE_INTERVAL INT          NOT NULL,  -- 비활성 만료 간격(초)
    EXPIRY_TIME           BIGINT       NOT NULL,  -- 만료 예정 시각 (만료 세션 청소용)
    PRINCIPAL_NAME        VARCHAR(100),           -- 로그인 사용자 식별자 (있을 때만)
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

-- SESSION_ID로 세션 조회 (가장 빈번) → 유니크 인덱스
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
-- 만료 세션 일괄 정리 시 EXPIRY_TIME 범위 검색
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
-- 특정 사용자의 세션 목록 조회
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

-- 세션 속성: 세션 1건에 담긴 값들 = N rows (SecurityContext, OAuth 인증정보 등)
CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,  -- FK → SPRING_SESSION.PRIMARY_ID
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,  -- 세션 속성 키
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,  -- 직렬화된 속성 값
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    -- 세션 삭제 시 속성도 함께 삭제 (CASCADE)
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);