package com.example.steam.repository;

import com.example.steam.entity.SocialLogin;
import com.example.steam.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {
    // 사용자와 소셜 코드를 기반으로 소셜 로그인 정보 조회
    Optional<SocialLogin> findByUserAndSocialCode(User user, Integer socialCode);
}