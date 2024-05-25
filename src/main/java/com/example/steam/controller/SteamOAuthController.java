package com.example.steam.controller;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@RequestMapping("/oauth/steam")
@RestController
public class SteamOAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SteamOAuthController.class);

    private final UserService userService;
    private final SteamAuthenticationService steamService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Autowired
    public SteamOAuthController(UserService userService, SteamAuthenticationService steamService, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.userService = userService;
        this.steamService = steamService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Autowired
    private SteamAuthenticationService steamAuthService;

    @GetMapping("/login")
    public ResponseEntity<?> getSteamLoginUrl() {
        String redirectUrl = "https://localhost:3000/HandleSteamCallback"; // 콜백 URL
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
            logger.error("Steam 로그인 URL 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }

    @GetMapping("/callback")
    public void handleSteamCallback(@RequestParam Map<String, String> params, HttpServletResponse response) {
        try {
            String claimedId = params.get("openid.claimed_id");
            String steamId = steamAuthService.extractSteamId(claimedId);

            if (!steamAuthService.validateSteamResponse(params)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "스팀 인증 실패");
                return;
            }

            String steamNickname = steamAuthService.getSteamNickname(steamId);

            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 실제로 CustomUserDetails 객체를 가져오는지 확인하는 로깅
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                logger.error("Principal is not of type CustomUserDetails: {}", authentication.getPrincipal().getClass());
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Authentication Principal type mismatch");
                return;
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findByUserId(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

            // 사용자의 Steam ID 업데이트 및 isSteamLinked 필드 설정
            user.setSteamId(steamId);
            user.setIsSteamLinked(true);

            // 업데이트된 사용자 정보 저장
            userRepository.save(user);

            // 새로운 사용자 정보로 토큰 생성
            String token = jwtTokenProvider.generateToken(authentication).getAccessToken();
            String redirectUrl = "https://localhost:3000/?accessToken=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                    "&claimedId=" + URLEncoder.encode(claimedId, StandardCharsets.UTF_8) +
                    "&steamNickname=" + URLEncoder.encode(steamNickname, StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            logger.error("Steam authentication failed", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Steam authentication failed");
            } catch (IOException ioException) {
                logger.error("리다이렉트 실패", ioException);
            }
        }
    }
}