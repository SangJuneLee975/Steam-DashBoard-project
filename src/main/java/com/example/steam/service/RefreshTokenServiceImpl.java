package com.example.steam.service;

import com.example.steam.dto.User;
import com.example.steam.entity.RefreshToken;
import com.example.steam.repository.RefreshTokenRepository;
import com.example.steam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }


    // 리프레시 토큰 생성
    @Override
    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        User user = userRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusWeeks(1)); // 1주 후 만료

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public boolean validateRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        boolean isValid = refreshToken.isPresent() && refreshToken.get().getExpiresAt().isAfter(LocalDateTime.now());

        if (isValid) {
            logger.info("리프레시 토큰 검증 성공: 토큰 = {}", token);
        } else {
            logger.error("리프레시 토큰 검증 실패 또는 만료됨: 토큰 = {}", token);
        }
        return isValid;
    }

    @Override
    public String getUserIdFromRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
        if (refreshTokenOpt.isPresent() && refreshTokenOpt.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            // 토큰이 존재하고, 아직 만료되지 않았다면, 관련된 사용자 ID를 반환
            logger.info("리프레시 토큰으로부터 사용자 ID 추출 성공: 토큰 = {}", token);
            return refreshTokenOpt.get().getUser().getUserId();
        } else {
            throw new RuntimeException("유효하지 않거나 만료된 리프레시 토큰");
        }
    }
}