package com.example.steam.controller;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
        String redirectUrl = "https://localhost:3000/oauth/steam/callback"; // 콜백 URL
        try {
            String loginUrl = steamService.buildSteamLoginUrl(redirectUrl);
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
    public ResponseEntity<?> handleSteamCallback(@RequestParam Map<String, String> params) {
        try {
            String assocHandle = params.get("openid.assoc_handle");
            String claimedId = params.get("openid.claimed_id");
         //   String signature = params.get("openid.sig");

            // Steam 응답을 검증하고 사용자 정보를 추출
            boolean isValid = steamService.validateSteamResponse(params);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("스팀 인증 실패");
            }


            // Steam ID를 기반으로 사용자를 찾거나 새로 생성
            String steamId = steamService.extractSteamId(claimedId);
            User user = userService.findOrCreateSteamUser(steamId);
      //      String steamNickname = steamService.getSteamNickname(steamId); // 스팀 닉네임 추출

            CustomUserDetails userDetails = new CustomUserDetails(
                    user.getUsername(),
                    user.getPassword(),
                    user.getName(),
                    user.getSocialCode(),
                    user.getAuthorities()
            );

            // 로그인된 사용자가 있으면 기존 사용자 계정에 Steam 계정을 연동
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails currentUserDetails = (CustomUserDetails) authentication.getPrincipal();
                User existingUser = userRepository.findByUserId(currentUserDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                steamService.linkSteamAccount(existingUser, steamId);
            } else {
                authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            String token = jwtTokenProvider.generateToken(authentication).getAccessToken();
            String redirectUrl = "https://localhost:3000/?accessToken=" + token;

            return ResponseEntity.ok(Map.of("accessToken", token, "claimedId", claimedId, "assocHandle", assocHandle, "redirectUrl", redirectUrl));
        } catch (Exception e) {
            logger.error("Steam authentication failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam authentication failed");
        }
    }
}