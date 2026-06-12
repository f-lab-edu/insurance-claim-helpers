package com.swk.claimhelpers.user.service;

import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.entity.UserOauthAccount;
import com.swk.claimhelpers.user.repository.UserOauthAccountRepository;
import com.swk.claimhelpers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthUserService {

    private final UserRepository userRepository;
    private final UserOauthAccountRepository oauthAccountRepository;

    /**
     * 프로바이더 + 식별키로 회원을 찾고, 없으면 신규 가입시킨다.
     *
     * @param provider OAuth 프로바이더 식별자 (예: "google")
     * @param oauthKey 프로바이더가 발급한 고유 식별자 (구글의 sub)
     * @param email    사용자 이메일
     * @return 기존 또는 신규 생성된 User
     */
    @Transactional
    public User findOrCreate(String provider, String oauthKey, String email) {
        // (provider, oauthKey) 복합 유니크로 식별 — 이미 가입한 계정이면 연결된 User를 그대로 반환
        return oauthAccountRepository.findByProviderAndOauthKey(provider, oauthKey)
                .map(UserOauthAccount::getUser)
                .orElseGet(() -> createUser(provider, oauthKey, email));
    }

    /**
     * 신규 회원 생성: User → UserOauthAccount 순으로 저장한다.
     * 둘은 한 트랜잭션 안에서 처리되어, 중간에 실패하면 함께 롤백된다(고아 User 방지).
     */
    private User createUser(String provider, String oauthKey, String email) {
        User user = userRepository.save(User.create(email));
        oauthAccountRepository.save(UserOauthAccount.create(user, provider, oauthKey));
        return user;
    }
}