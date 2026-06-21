package com.swk.claimhelpers.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * DB 테스트 공용 베이스 클래스.
 *
 * <p>외부 PostgreSQL(localhost:5432)에 의존하지 않고 테스트가 컨테이너를 직접 띄운다.
 * 이 클래스를 상속하면 datasource 가 자동으로 컨테이너로 연결된다.
 *
 * <p>설계 결정
 * <ul>
 *   <li><b>pgvector 이미지</b>: pgvector 확장이 내장된 {@code pgvector/pgvector:pg16} 를 쓰고 
 *       {@code asCompatibleSubstituteFor("postgres")} 로
 *       Testcontainers 에 'postgres 호환'임을 알려 {@link PostgreSQLContainer} 로 다룬다.
 *   <li><b>싱글톤 컨테이너</b>: {@code @Testcontainers}/{@code @Container}(클래스 단위 생명주기) 대신
 *       static 필드 + static 초기화 블록에서 한 번만 {@code start()} 한다. JVM 당 1개만 기동되어
 *       이 클래스를 상속한 모든 테스트 클래스가 공유하므로 빠르다. 컨테이너 종료는 Testcontainers
 *       Ryuk 가 JVM 종료 시 자동 정리하므로 별도 stop 이 필요 없다.
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
     * 컨테이너의 동적 접속 정보를 Environment 에 주입한다.
     * (OAuth client 자격증명은 @DataJpaTest 슬라이스가 Security 자동설정을 로드하지 않아 불필요하다.)
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}