package com.example.steam.controller;

import com.example.steam.service.SteamAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/oauth/steam")
public class SteamOAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SteamOAuthController.class);

    @Autowired
    private SteamAuthenticationService steamAuthService;

    @GetMapping("/login")
    public ResponseEntity<?> getSteamLoginUrl() {
        String redirectUrl = "https://localhost:3000/oauth/steam/callback"; // 필요에 따라 조정
        try {
            String loginUrl = steamAuthService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("steamLoginUrl", loginUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }


    @GetMapping("/connect")
    public ResponseEntity<?> getSteamConnectUrl() {
        try {
            String redirectUrl = "https://localhost:8080/oauth/steam/callback";
            String loginUrl = steamAuthService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("url", loginUrl));
        } catch (Exception e) {
            logger.error("Error creating Steam login URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Steam login URL");
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<?> handleSteamCallback(@RequestParam("steamId") String steamId, @RequestParam("displayName") String displayName) {
        try {
            steamAuthService.processSteamUser(steamId, displayName);
            return ResponseEntity.ok("Steam 사용자 처리 성공.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 사용자 처리 실패");
        }
    }
}