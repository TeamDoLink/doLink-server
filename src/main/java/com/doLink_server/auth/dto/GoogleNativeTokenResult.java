package com.doLink_server.auth.dto;

/**
 * 네이티브 Google 로그인 성공 시 앱(WebView)으로 내려주는 토큰 쌍
 */
public record GoogleNativeTokenResult(String accessToken, String refreshToken) {
}
