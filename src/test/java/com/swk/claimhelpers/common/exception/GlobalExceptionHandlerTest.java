package com.swk.claimhelpers.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void CustomException은_ErrorCode의_status와_기본메시지를_반환한다() throws Exception {
        mockMvc.perform(get("/test/custom-default"))
                .andExpect(status().isForbidden()) // FORBIDDEN = 403
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    @Test
    void CustomException의_override_메시지가_응답에_반영된다() throws Exception {
        // (ErrorCode, String) 생성자로 넘긴 메시지가 기본 메시지 대신 노출되어야 한다.
        mockMvc.perform(get("/test/custom-override"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("이 약관은 다른 사용자의 것입니다."));
    }

    @Test
    void 검증_실패는_400_BAD_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 요청_본문_파싱_실패는_400_BAD_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 경로변수_타입_불일치는_400_BAD_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(get("/test/mismatch/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 지원하지_않는_메서드는_405_METHOD_NOT_ALLOWED를_반환한다() throws Exception {
        mockMvc.perform(post("/test/custom-default"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 예상하지_못한_예외는_500_INTERNAL_ERROR를_반환하고_내부정보를_노출하지_않는다() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError()) // INTERNAL_ERROR = 500
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                // 원본 예외 메시지("DB 커넥션 실패: ...")가 아니라 일반 메시지만 노출되어야 한다.
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    /**
     * 각 예외 케이스를 던지기 위한 테스트 전용 컨트롤러.
     */
    @RestController
    static class TestController {

        @GetMapping("/test/custom-default")
        public void customDefault() {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        @GetMapping("/test/custom-override")
        public void customOverride() {
            throw new CustomException(ErrorCode.FORBIDDEN, "이 약관은 다른 사용자의 것입니다.");
        }

        @GetMapping("/test/validation")
        public void validation() throws NoSuchMethodException, MethodArgumentNotValidException {
            // @Valid 바인딩 없이도 핸들러를 검증하기 위해 MethodArgumentNotValidException 을 직접 구성한다.
            Method method = TestController.class.getMethod("validation");
            MethodParameter parameter = new MethodParameter(method, -1);
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "field", "비어 있을 수 없습니다."));
            throw new MethodArgumentNotValidException(parameter, bindingResult);
        }

        @GetMapping("/test/unexpected")
        public void unexpected() {
            // 핸들러가 잡지 않은 일반 예외 → 500 fallback 경로로 흘러가야 한다.
            throw new RuntimeException("DB 커넥션 실패: jdbc:postgresql://internal-host:5432");
        }

        @PostMapping("/test/body")
        public void body(@RequestBody Payload payload) {
        }

        @GetMapping("/test/mismatch/{id}")
        public void mismatch(@PathVariable Long id) {
        }

        record Payload(String name) {
        }
    }
}