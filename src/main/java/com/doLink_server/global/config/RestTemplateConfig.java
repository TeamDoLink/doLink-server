package com.doLink_server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Rest API 설정 클래스
 *
 * - Spring이 관리하는 객체(Bean)로 RestTemplate를 등록
 * - Spring 컨테이너가 관리하도록 하여 재사용 및 싱글톤 유지
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}