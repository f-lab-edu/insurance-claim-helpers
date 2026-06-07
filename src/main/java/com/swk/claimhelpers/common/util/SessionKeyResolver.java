package com.swk.claimhelpers.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 비로그인(익명) 사용자를 식별하기 위한 session_key 추출 유틸.
 */
@Component
public class SessionKeyResolver {
    
    public String resolve(HttpServletRequest request) {
        return request.getSession(true).getId();
    }
}