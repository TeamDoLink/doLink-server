package com.doLink_server.auth.oauth.model;


import com.doLink_server.security.dto.UserDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 스프링 시큐리티 인증용 UserDetails 구현체
 */
@Getter
public class CustomOAuth2UserDetails implements UserDetails {

    private final String userId;

    /**
     * 생성자
     * @param userDTO 인증 시 사용자 식별 정보 전달용 DTO
     */
    public CustomOAuth2UserDetails(UserDTO userDTO) {
        this.userId = userDTO.getUserId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_USER 권한 고정으로 줌. 필요하면 확장 가능
        return Collections.singleton(() -> "ROLE_USER");
    }

    @Override
    public String getPassword() {
        return null; // JWT 인증에서는 비밀번호 사용 안 함
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 필요하면 변경 가능
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 필요하면 변경 가능
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 필요하면 변경 가능
    }

    @Override
    public boolean isEnabled() {
        return true; // 필요하면 변경 가능
    }
}