package com.doLink_server.security.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 토큰 생성 및 파싱 클래스
 */
@Slf4j
@Component
public class JwtProvider {

    // 서명을 위한 SecretKey 객체 (HMAC 방식)
    private SecretKey secretKey;

    @Value("${spring.jwt.secret}")
    private String secret;

    private final ObjectMapper objectMapper;

    public JwtProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 생성자 후 초기화: secret 값을 SecretKey 객체로 변환
     */
    @PostConstruct
    protected void init() {
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        // secret 문자열을 바이트로 변환하여 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);
        log.info("[JwtUtil:init] secret(raw): {}", secret);
        log.info("[JwtUtil:init] secret(decoded base64 length): {}", decodedKey.length);
    }

    /**
     * JWT 토큰에서 Claims 파싱 (서명 검증 포함)
     * @param token JWT 토큰 문자열
     * @return Claims 객체
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료됐어도 Claims는 반환
            return e.getClaims();
        }
    }

    /**
     * 토큰에서 userId 추출
     * @param token JWT 토큰 문자열
     * @return userId 클레임 값(UUID)
     */
    public String getUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            String idStr = claims.get("userId", String.class);

            if (idStr == null) {
                throw new IllegalArgumentException("userId 클레임이 존재하지 않습니다.");
            }

            return idStr;
        } catch (Exception e) {
            // 여기서 로그로 디버깅
            log.error("JWT에서 userId 추출 실패 token : {}", token);
            log.error("JWT에서 userId 추출 실패: {}", e.getMessage(), e);
            throw e; // 나중에 커스텀 예외로 변경 가능
        }
    }

    /**
     * 토큰에서 category(access/refresh) 추출
     * @param token JWT 토큰 문자열
     * @return category 클레임 값
     */
    public String getCategory(String token) {
        return parseClaims(token).get("category", String.class);
    }

    /**
     * 토큰 만료 여부 확인
     * @param token JWT 토큰 문자열
     * @return true: 만료됨, false: 아직 유효함
     */
    public boolean isExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    /**
     * JWT 토큰 생성
     * @param userId 사용자 아이디(UUID)
     * @param expiredMs 토큰 유효 시간 (밀리초 단위)
     * @param category access 또는 refresh 등 토큰 용도
     * @return 서명된 JWT 토큰 문자열
     */
    public String createJwt(String userId, Long expiredMs, String category) {
        log.info("NEW 토큰 발행 합니다 !!!!!!!!!!!!!! ");
        return Jwts.builder()
                .claim("userId", userId) // UUID를 문자열로 넣기
                .claim("category", category)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 만료 시각
                .signWith(secretKey) // 서명
                .compact();          // JWT 문자열 생성
    }

    /**
     * 토큰 유효성 검증 (서명 검사)
     * @param token JWT 토큰 문자열
     * @return true: 유효함, false: 유효하지 않음
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT 유효성 검사 실패: {}", e.getMessage()); // 꼭 찍어야 함
            return false;
        }
    }

    /**
     * JWT 토큰의 Claims(payload)를 JSON 형식으로 반환한다.
     * @param token JWT 토큰 문자열
     * @return Claims JSON 문자열, 파싱 실패시 Optional.empty()
     */
    public Optional<String> extractClaimsAsJson(String token) {
        try {
            Claims claims = parseClaims(token);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
            return Optional.of(json);
        } catch (JwtException | JsonProcessingException e) {
            return Optional.empty();
        }
    }
}