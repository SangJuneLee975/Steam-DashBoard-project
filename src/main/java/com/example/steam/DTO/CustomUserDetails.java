package com.example.steam.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private String username;
    private String password;
    private String name;
    private Collection<? extends GrantedAuthority> authorities;

    // User 엔티티를 기반으로 CustomUserDetails 객체를 생성하는 생성자
    public CustomUserDetails(String username, String password, String name, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getName() {
        return name; // 이름 반환 메서드
    }

    public void setName(String name) {
        this.name = name;
    }
    // 계정의 만료, 잠금, 비밀번호 만료, 활성화 상태를 반환하는 메서드들

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
