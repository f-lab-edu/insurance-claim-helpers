package com.swk.claimhelpers.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuthUserService oauthUserService;

    // Spring 기본 OIDC 사용자 로더. 표준 로딩(구글 userinfo 호출 등)은 여기에 위임한다.
    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1) Spring 기본 서비스로 구글 사용자 정보를 로드 (id_token 파싱 + userinfo 호출)
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2) 회원 식별/저장에 필요한 값 추출
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String oauthKey = oidcUser.getSubject();  // 구글이 부여한 고유 식별자 (sub)
        String email = oidcUser.getEmail();

        oauthUserService.findOrCreate(provider, oauthKey, email);

        // 3) 로드한 OidcUser를 그대로 반환 → Spring이 세션에 저장하며 로그인 완료
        return oidcUser;
    }
}