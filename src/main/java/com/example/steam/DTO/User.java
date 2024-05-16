package com.example.steam.dto;

import com.example.steam.entity.RefreshToken;
import com.example.steam.entity.Role;
import com.example.steam.entity.SocialLogin;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 생성 설정
    private Long id;    //인덱스 값

    @Column(unique = true) // 유니크 제약 조건 추가
    private String userId; // 사용자 ID

    private String password;
    private String name;
    @Column(nullable = true)
    private String nickname;
    private String email;

    @Transient
    private Integer socialCode;

    public void setSocialCode(Integer socialCode) {
        this.socialCode = socialCode;
    }

    @Transient
    private boolean isSocial; // 소셜 로그인 사용자인지 구분

    @Transient
    private String passwordConfirm; // 비밀번호 확인
    public void setNickname(String nickname) {
        this.nickname = nickname;  // 닉네임 설정
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }



    public void setSocial(boolean isSocial) {
        this.isSocial = isSocial;
    }

    @Transient
    private Collection<? extends GrantedAuthority> authorities;

    // UserDetails 인터페이스 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }


    // User와 SocialLogin 간의 1:N 관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<SocialLogin> socialLogins = new HashSet<>();

    public void addSocialLogin(SocialLogin socialLogin) {
        socialLogins.add(socialLogin);
        socialLogin.setUser(this);
    }

    public void removeSocialLogin(SocialLogin socialLogin) {
        socialLogins.remove(socialLogin);
        socialLogin.setUser(null);
    }

    // user 엔티티와 role 엔티티 간에 다대다 관계(@ManyToMany)를 설정
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();


    // 권한(역할) 추가 메서드
    public void addRole(Role role) {
        this.roles.add(role);
    }

    // 권한(역할) 설정 메서드
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // Builder 패턴 적용
    @Builder
    public User(String userId, String password, String name, String nickname, String email, boolean isSocial) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.isSocial = isSocial;


    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // 정상적으로 직렬화 수행
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserId(String userId) {
        this.userId = userId; // 사용자 ID 설정
    }

    public void setSteamId(String steamId) {
        this.userId = steamId; // 스팀 ID를 사용자 ID로 설정
    }

}
