package com.example.steam.controller;

import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.entity.OAuthTokens;
import com.example.steam.entity.RefreshToken;
import com.example.steam.model.GoogleUser;
import com.example.steam.model.NaverUser;
import com.example.steam.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private NaverOAuthService naverOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private NaverUserService naverUserService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @GetMapping("/google/login")
    public String googleLogin() {
        return googleOAuthService.getGoogleAuthorizationUrl();
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) {
        logger.info("Google OAuth 콜백 처리 시작: code={}", code);
        try {
            OAuthTokens tokens = googleOAuthService.getAccessToken(code);
            GoogleUser googleUser = googleOAuthService.getUserInfo(tokens.getAccessToken());

            User user = userService.processGoogleUser(googleUser, tokens.getAccessToken());
            String jwt = jwtTokenProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null, Collections.emptyList())).getAccessToken();
            String redirectUrlWithToken = "https://localhost:3000/?token=" + jwt;
            response.sendRedirect(redirectUrlWithToken);
        } catch (Exception e) {
            logger.error("Google OAuth 콜백 처리 중 오류 발생", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Google OAuth 처리 중 오류 발생");
            } catch (IOException ioException) {
                logger.error("리다이렉트 실패", ioException);
            }
        }
    }

    @GetMapping("/naver/login")
    public String naverLogin() {
        return naverOAuthService.getNaverAuthorizationUrl();
    }

    @GetMapping("/naver/callback")
    public void naverCallback(@RequestParam("code") String code, @RequestParam("state") String state, HttpServletResponse response) {
        try {
            OAuthTokens tokens = naverOAuthService.getAccessToken(code, state);
            NaverUser naverUser = naverOAuthService.getUserInfo(tokens.getAccessToken());

            User user = naverUserService.processNaverUser(naverUser, tokens.getAccessToken());


            // JWT 토큰 생성
            String jwt = jwtTokenProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null, Collections.emptyList())).getAccessToken();

            // 클라이언트에 전달할 토큰 정보
            String redirectUrlWithToken = "https://localhost:3000/?token=" + jwt;
            response.sendRedirect(redirectUrlWithToken);
        } catch (Exception e) {
            logger.error("Naver OAuth 콜백 처리 중 오류 발생", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Naver OAuth 처리 중 오류 발생");
            } catch (IOException ioException) {
                logger.error("리다이렉트 실패", ioException);
            }
        }
    }
}