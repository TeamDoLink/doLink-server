package com.doLink_server.security.jwt;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 프로퍼티
 */
@Component
@Getter
public class JwtProperties {

    @Value("${spring.jwt.access-expiration-time}")
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-expiration-time}")
    private long refreshTokenExpiration;
}