package com.swk.claimhelpers.common.config;

import com.swk.claimhelpers.user.service.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOidcUserService customOidcUserService) throws Exception {
        http
                // (1) URL별 접근 권한: 로그인 필수 vs 공개
                .authorizeHttpRequests(auth -> auth
                        // api-spec 기준 로그인 필수 엔드포인트 (정확한 경로 매칭 — 하위 경로는 공개)
                        .requestMatchers(HttpMethod.GET, "/api/claim-criteria").authenticated()   // 내 약관 목록
                        .requestMatchers(HttpMethod.GET, "/api/chat/sessions").authenticated()     // 내 상담 내역
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()          // 내 정보
                        // 나머지는 모두 공개 — 비로그인 사용자도 업로드/채팅 등 핵심 기능을 써야 한다.
                        .anyRequest().permitAll()
                )
                // (2) 구글 OAuth2 로그인 활성화 + 커스텀 URL 연결
                .oauth2Login(oauth -> oauth
                        // (기본값 /oauth2/authorization 를 /auth 로 교체)
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/auth"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/auth/google/callback"))
                        // 사용자 정보 로드 시 우리 커스텀 서비스로 회원 find-or-create 수행
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                )
                // (3) 로그아웃: POST /auth/logout 요청 시 세션을 무효화하고 204(No Content) 반환
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .invalidateHttpSession(true)
                )
                // (4) CSRF 비활성화
                // TODO: 세션 쿠키 기반이므로 프론트 연동 시 쿠키 기반 CSRF 토큰 방식으로 다시 켜야 한다.
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}