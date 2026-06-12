package com.swk.claimhelpers.user.service;

import com.swk.claimhelpers.support.AbstractPostgresContainerTest;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.repository.UserOauthAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OAuthUserService 통합 테스트 (실제 PostgreSQL 사용).
 *
 * 동작 전제:
 * - {@link AbstractPostgresContainerTest} 가 pgvector PostgreSQL 컨테이너를 자가 기동한다(Docker 필요).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OAuthUserService.class)
class OAuthUserServiceIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private OAuthUserService oauthUserService;

    @Autowired
    private UserOauthAccountRepository oauthAccountRepository;

    @Test
    @DisplayName("신규 로그인이면 User와 OAuth 계정이 실제로 저장되고 id가 부여된다")
    void 신규_로그인이면_실제로_저장되고_id가_부여된다() {
        // when
        User result = oauthUserService.findOrCreate("google", "sub-int-1", "int1@gmail.com");

        // then: DB가 부여한 id가 있고, OAuth 계정이 실제로 조회되며 같은 User에 연결돼 있다
        assertThat(result.getId()).isNotNull();
        assertThat(oauthAccountRepository.findByProviderAndOauthKey("google", "sub-int-1"))
                .isPresent()
                .get()
                .satisfies(account -> assertThat(account.getUser().getId()).isEqualTo(result.getId()));
    }

    @Test
    @DisplayName("같은 계정으로 다시 호출하면 중복 저장 없이 같은 User를 반환한다")
    void 같은_계정_재호출시_중복없이_같은_User를_반환한다() {
        // given: 첫 로그인으로 가입
        User first = oauthUserService.findOrCreate("google", "sub-int-2", "int2@gmail.com");

        // when: 같은 (provider, oauthKey)로 재로그인
        User second = oauthUserService.findOrCreate("google", "sub-int-2", "int2@gmail.com");

        // then: 새로 만들지 않고 동일 User를 반환 (id 동일)
        assertThat(second.getId()).isEqualTo(first.getId());
    }
}