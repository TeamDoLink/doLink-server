package com.doLink_server.security.config;

import com.doLink_server.auth.oauth.handler.CustomOAuth2ExceptionHandler;
import com.doLink_server.auth.oauth.handler.CustomOAuth2SuccessHandler;
import com.doLink_server.auth.oauth.service.CustomOAuth2UserService;
import com.doLink_server.auth.service.JwtIssueService;
import com.doLink_server.auth.service.RedisService;
import com.doLink_server.security.entrypoint.CustomAuthenticationEntryPoint;
import com.doLink_server.security.filter.CustomLogoutFilter;
import com.doLink_server.security.filter.JwtAuthenticationFilter;
import com.doLink_server.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * 보안정책 설정 클래스
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomOAuth2ExceptionHandler customOAuth2ExceptionHandler;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final JwtIssueService jwtIssueService;
    private final RedisService redisService;

    @Bean
    public JwtAuthenticationFilter jwtFilter() {
        return new JwtAuthenticationFilter(jwtProvider, redisService, jwtIssueService);
    }

    @Bean
    public CustomLogoutFilter customLogoutFilter() {
        return new CustomLogoutFilter(jwtProvider, objectMapper, jwtIssueService, redisService);
    }

    /**
     * Spring Security 필터 체인 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // CSRF 비활성화
        http.csrf(AbstractHttpConfigurer::disable);

        // 경로별 인가 설정
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(WHITE_LIST_URL).permitAll()
                .anyRequest().authenticated()
        );

        // 인증 실패 시 리다이렉트 없이 401 Unauthorized 상태코드 응답
        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper))
        );

        // JWT 필터 설정
        http.addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(customLogoutFilter(), LogoutFilter.class);

        // 세션 설정: STATELESS
        http.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));

        // OAuth2 로그인 설정
        http.oauth2Login(oauth2Configurer -> oauth2Configurer
                .successHandler(customOAuth2SuccessHandler)
                .failureHandler(customOAuth2ExceptionHandler)
                .userInfoEndpoint(userInfoEndpointConfig
                        -> userInfoEndpointConfig.userService(customOAuth2UserService))
        );

        http.cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(
                            "http://localhost:3000",
                            "https://api.dolink.team",
                            "https://app.dolink.team",
                            "https://dolink.team"
                    ));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * OAuth 콜백 URL을 처리하기 위한 인증 우회설정
     */
    private static final String[] WHITE_LIST_URL = {
            "/v1/auth/reissue",

            // swagger
            "/v3/api-docs/**",
            "/swagger-ui/**",

            // 프론트 jwt 개발배포환경
            "/temp/login"
    };
}