package com.swk.claimhelpers.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint =
            new RestAuthenticationEntryPoint(new ObjectMapper());

    @Test
    void 인증되지_않은_요청에_401과_UNAUTHORIZED_JSON_본문을_반환한다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("인증 정보 없음"));

        // then: 401 + 표준 에러 JSON
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }
}