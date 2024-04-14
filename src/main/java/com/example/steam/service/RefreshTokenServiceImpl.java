package com.example.steam.service;

import com.example.steam.DTO.User;
import com.example.steam.entity.RefreshToken;
import com.example.steam.repository.RefreshTokenRepository;
import com.example.steam.repository.UserRepository;
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

    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }


    @Override
    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusWeeks(1)); // 1주 후 만료

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public boolean validateRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        return refreshToken.isPresent() && refreshToken.get().getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    public String getUserIdFromRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
        if (refreshTokenOpt.isPresent() && refreshTokenOpt.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            // 토큰이 존재하고, 아직 만료되지 않았다면, 관련된 사용자 ID를 반환
            return refreshTokenOpt.get().getUser().getUserId();
        } else {
            throw new RuntimeException("유효하지 않거나 만료된 리프레시 토큰");
        }
    }
}