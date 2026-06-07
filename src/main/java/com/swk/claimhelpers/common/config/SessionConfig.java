package com.swk.claimhelpers.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableSpringHttpSession
public class SessionConfig {

    /**
     * 메모리(ConcurrentHashMap) 기반 세션 저장소.
     * - 장점: 외부 인프라 없이 바로 동작
     * - 한계: 서버 재시작 시 세션 소멸, 다중 서버 공유 불가 (그래서 운영에선 Redis/JDBC로 교체 예정)
     */
    @Bean
    public MapSessionRepository sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }
}