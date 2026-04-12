package com.doLink_server.auth.service;

import com.doLink_server.auth.dto.GoogleNativeTokenResult;
import com.doLink_server.auth.oauth.model.GoogleUserInfo;
import com.doLink_server.auth.oauth.model.OAuth2UserInfo;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.global.util.UUIDToBytesUtil;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Google ID 토큰 기반 네이티브 로그인 (React Native Google Sign-In)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleNativeAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleWebClientId;

    private final UserService userService;
    private final JwtIssueService jwtIssueService;
    private final RedisService redisService;

    public GoogleNativeTokenResult loginWithIdToken(String rawIdToken) {
        GoogleIdToken idToken = verifyGoogleIdToken(rawIdToken);
        GoogleIdToken.Payload payload = idToken.getPayload();

        if (payload.getSubject() == null || payload.getSubject().isBlank()) {
            throw new GeneralException(ErrorStatus._INVALID_GOOGLE_ID_TOKEN);
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", payload.getSubject());
        attributes.put("email", payload.getEmail());
        attributes.put("name", payload.get("name"));
        attributes.put("picture", payload.get("picture"));
        OAuth2UserInfo userInfo = new GoogleUserInfo(attributes);

        Users user = resolveOrRegisterUser(userInfo);

        String userId = UUIDToBytesUtil.convertToEntityAttribute(user.getUserId()).toString();
        String refreshToken = jwtIssueService.issueRefreshToken(userId);
        redisService.saveRefreshToken(userId, refreshToken);
        String accessToken = jwtIssueService.issueAccessToken(userId);

        return new GoogleNativeTokenResult(accessToken, refreshToken);
    }

    private Users resolveOrRegisterUser(OAuth2UserInfo userInfo) {
        try {
            return userService.findExistingUser(userInfo);
        } catch (GeneralException e) {
            if (e.getCode() == ErrorStatus._NOT_FOUND_USER) {
                return userService.join(userInfo);
            }
            throw e;
        }
    }

    private GoogleIdToken verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleWebClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idTokenString);
            if (token == null) {
                throw new GeneralException(ErrorStatus._INVALID_GOOGLE_ID_TOKEN);
            }
            return token;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID token verification failed: {}", e.getMessage());
            throw new GeneralException(ErrorStatus._INVALID_GOOGLE_ID_TOKEN);
        }
    }
}
