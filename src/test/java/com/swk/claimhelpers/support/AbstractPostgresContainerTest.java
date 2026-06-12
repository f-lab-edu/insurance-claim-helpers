package com.swk.claimhelpers.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * DB 통합 테스트 공용 베이스 클래스.
 *
 * <p>외부 PostgreSQL(localhost:5432)에 의존하지 않고, 테스트가 컨테이너를 직접 띄운다.
 * 이 클래스를 상속하면 datasource 가 자동으로 컨테이너로 연결된다.
 *
 * <p>설계 결정
 * <ul>
 *   <li><b>pgvector 이미지</b>: 마이그레이션 V8(vector_store)이 {@code CREATE EXTENSION vector} 를
 *       사용하므로 기본 {@code postgres} 이미지로는 Flyway 가 실패한다. pgvector 확장이 내장된
 *       {@code pgvector/pgvector:pg16} 를 쓰고, {@code asCompatibleSubstituteFor("postgres")} 로
 *       Testcontainers 에 'postgres 호환'임을 알려 {@link PostgreSQLContainer} 로 다룬다.
 *   <li><b>싱글톤 컨테이너</b>: {@code @Testcontainers}/{@code @Container}(클래스 단위 생명주기) 대신,
 *       static 필드 + static 초기화 블록에서 한 번만 {@code start()} 한다. JVM 당 1개만 기동되어
 *       이 클래스를 상속한 모든 테스트 클래스가 공유하므로 빠르다. 컨테이너 종료는 Testcontainers
 *       Ryuk 가 JVM 종료 시 자동 정리하므로 별도 stop 이 필요 없다.
 *   <li><b>기본 'local' 프로필 유지</b>: 별도 test 프로필을 만들지 않는다. S3Client 빈이
 *       {@code @Profile("local"/"prod")} 로만 생성되기 때문에, test 프로필을 켜면 S3FileStorage 가
 *       의존하는 S3Client 빈이 사라져 {@code @SpringBootTest} 컨텍스트 로딩이 깨진다. 그래서
 *       local 에서 깨지는 외부 의존(DB host, OAuth client 자격증명)만 아래에서 덮어쓴다.
 * </ul>
 */
public abstract class AbstractPostgresContainerTest {

    // static: JVM 당 1개. 모든 하위 테스트 클래스가 이 단일 컨테이너를 공유한다.
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor("postgres"));

    static {
        // @Container 미사용이므로 직접 기동한다(전체 테스트 동안 1회). 정리는 Ryuk 가 담당.
        POSTGRES.start();
    }

    /**
     * 컨테이너의 동적 접속 정보와, local 프로필에서 비어 있는 OAuth 더미 값을 Environment 에 주입한다.
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.security.oauth2.client.registration.google.client-id",
                () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret",
                () -> "test-client-secret");
    }
}