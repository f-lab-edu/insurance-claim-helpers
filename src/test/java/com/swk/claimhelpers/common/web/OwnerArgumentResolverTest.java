package com.swk.claimhelpers.common.web;

import com.swk.claimhelpers.common.util.SessionKeyResolver;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OwnerArgumentResolverTest {

    @Mock
    private UserService userService;

    @Mock
    private SessionKeyResolver sessionKeyResolver;

    @Mock
    private NativeWebRequest webRequest;

    @InjectMocks
    private OwnerArgumentResolver resolver;

    @AfterEach
    void 컨텍스트_정리() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("파라미터 타입이 Owner 면 지원한다")
    void 파라미터_Owner타입_지원() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(ownerParameter())).isTrue();
    }

    @Test
    @DisplayName("파라미터 타입이 Owner 가 아니면 지원하지 않는다")
    void 파라미터_다른타입_미지원() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(stringParameter())).isFalse();
    }

    @Test
    @DisplayName("로그인(OidcUser) 요청이면 email 로 User 를 조회해 Owner.user 만 채운다")
    void 로그인_요청이면_user() {
        OidcUser principal = mock(OidcUser.class);
        given(principal.getEmail()).willReturn("a@gmail.com");
        setAuthentication(principal);
        User user = User.create("a@gmail.com");
        given(userService.getByEmail("a@gmail.com")).willReturn(user);

        Owner owner = (Owner) resolver.resolveArgument(null, null, webRequest, null);

        assertThat(owner.user()).isEqualTo(user);
        assertThat(owner.sessionKey()).isNull();
    }

    @Test
    @DisplayName("비로그인 요청이면 HTTP 세션 ID 를 session_key 로만 채운다")
    void 비로그인_요청이면_sessionKey() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(webRequest.getNativeRequest(HttpServletRequest.class)).willReturn(request);
        given(sessionKeyResolver.resolve(request)).willReturn("sess-1");

        Owner owner = (Owner) resolver.resolveArgument(null, null, webRequest, null);

        assertThat(owner.user()).isNull();
        assertThat(owner.sessionKey()).isEqualTo("sess-1");
    }

    private void setAuthentication(Object principal) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        SecurityContextHolder.setContext(context);
    }

    private MethodParameter ownerParameter() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("handleOwner", Owner.class);
        return new MethodParameter(method, 0);
    }

    private MethodParameter stringParameter() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("handleString", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private static class Sample {
        void handleOwner(Owner owner) {
        }

        void handleString(String value) {
        }
    }
}