package com.doLink_server.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 앱 네이티브 Google Sign-In 후 서버로 전달하는 요청
 */
public record GoogleNativeLoginRequest(@NotBlank String idToken) {
}
