package com.doLink_server.auth.service;


import com.doLink_server.security.jwt.JwtProperties;
import com.doLink_server.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * redis 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    /**
     * Redis 키 형식 정의
     * @param userId 사용자 아이디
     * @return refresh token key
     */
    private String buildKey(String userId) {
        return "refresh:" + userId;
    }

    /**
     * Redis에 리프레시 토큰 삭제
     * @param userId 사용자 식별자
     */
    public void removeRefreshToken(String userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
    }

    /**
     * Redis에 refresh token 저장
     * @param userId       사용자 UUID
     * @param refreshToken 저장할 refresh token 값
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        long expirationMs = jwtProperties.getRefreshTokenExpiration();
        String key = buildKey(userId);
        redisTemplate.opsForValue().set(key, refreshToken, expirationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Redis에서 userId에 해당하는 리프레시 토큰 조회
     * @param userId 사용자 식별자 (UUID의 문자열 표현)
     * @return 저장된 리프레시 토큰, 없으면 null 반환
     */
    public String getRefreshToken(String userId) {
        String token = getTokenFromRedis(userId);
        log.info("Cookie stored token: '{}'", token);
        return token;
    }

    /**
     * Redis에 저장된 리프레시 토큰과 비교하여 유효성 체크
     *
     * @param userId 사용자 식별자 (UUID의 문자열 표현)
     * @param refreshToken 클라이언트에서 전달받은 리프레시 토큰
     * @return 유효하지 않으면 true, 유효하면 false
     */
    public boolean isInvalidRefreshToken(String userId, String refreshToken) {
        String stored = getTokenFromRedis(userId);

        // Redis에 저장된 토큰이 없거나, 토큰 자체가 유효하지 않거나, 두 토큰이 일치하지 않으면 유효하지 않음
        return stored == null
                || !jwtProvider.validateToken(stored)
                || !jwtProvider.validateToken(refreshToken)
                || !stored.equals(refreshToken);
    }

    /**
     * refreshToken을 조회하는 공통 메서드
     * @param userId 사용자 아이디
     * @return refreshToken
     */
    private String getTokenFromRedis(String userId) {
        String key = buildKey(userId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 소셜 refresh token 저장
     */
    public void saveSocialRefreshToken(String userId, String token) {
        redisTemplate.opsForValue().set("social_refresh:" + userId, token);
    }

    /**
     * 소셜 refresh token 조회
     */
    public String getSocialRefreshToken(String userId) {
        return redisTemplate.opsForValue().get("social_refresh:" + userId);
    }

    /**
     * 소셜 refresh token 삭제
     */
    public void removeSocialRefreshToken(String userId) {
        redisTemplate.delete("social_refresh:" + userId);
    }
}