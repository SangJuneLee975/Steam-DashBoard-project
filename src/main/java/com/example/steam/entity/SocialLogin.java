package com.example.steam.entity;

import com.example.steam.dto.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_social_login")
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class SocialLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 필드는 매핑된 User 엔티티의 참조를 직접 저장합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer socialCode; // 소셜 로그인 제공자 구분 코드
    private String externalId; // 소셜 로그인 제공자로부터 받은 사용자 고유 ID
    private String accessToken; // 액세스 토큰 값

    private LocalDateTime updateDate;

    // 빌더 패턴 적용
    @Builder
    public SocialLogin(User user, Integer socialCode, String externalId, String accessToken, LocalDateTime updateDate) {
        this.user = user;
        this.socialCode = socialCode;
        this.externalId = externalId;
        this.accessToken = accessToken;
        this.updateDate = updateDate;

    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUser(User user) {
        this.user = user;
        if (!user.getSocialLogins().contains(this)) {
            user.getSocialLogins().add(this);
        }
    }
}
