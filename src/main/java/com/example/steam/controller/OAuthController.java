package com.example.steam.controller;

import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.GoogleUser;
import com.example.steam.service.GoogleOAuthService;
import com.example.steam.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @GetMapping("/google/login")
    public String googleLogin() {
        return googleOAuthService.getGoogleAuthorizationUrl();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code) {
        logger.info("Google OAuth 콜백 처리 시작: code={}", code);
        try {
            OAuthTokens tokens = googleOAuthService.getAccessToken(code);
            GoogleUser googleUser = googleOAuthService.getUserInfo(tokens.getAccessToken());

            // GoogleUser 정보를 기반으로 회원가입 또는 로그인 처리
            User user = userService.processGoogleUser(googleUser);

            // 사용자 인증 정보를 기반으로 JWT 토큰 생성
            String jwt = jwtTokenProvider.generateToken(new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, Collections.emptyList())).getAccessToken();

            // 토큰 반환
            return ResponseEntity.ok().body(Map.of("accessToken", jwt));
        } catch (IOException e) {
            logger.error("Google OAuth 콜백 처리 중 오류 발생", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Google OAuth 처리 중 오류 발생");
        }
    }

}