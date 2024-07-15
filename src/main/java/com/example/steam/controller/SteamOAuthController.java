package com.example.steam.controller;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.SteamLinkRequest;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.CustomUserDetailsService;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;



@RequestMapping("/oauth/steam")
@RestController
public class SteamOAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SteamOAuthController.class);

    @Value("${steam.api.key}")
    private String steamApiKey;

    @Value("${steam.api.id}")
    private String steamApiId;

    @Value("${steam.api.url}")
    private String steamApiUrl;      // https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/

    private final UserService userService;
    private final SteamAuthenticationService steamService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final RestTemplate restTemplate;

    @Autowired
    public SteamOAuthController(UserService userService, SteamAuthenticationService steamService, JwtTokenProvider jwtTokenProvider, UserRepository userRepository, CustomUserDetailsService customUserDetailsService, RestTemplate restTemplate) {
        this.userService = userService;
        this.steamService = steamService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.restTemplate = restTemplate;
    }


    @Autowired
    private SteamAuthenticationService steamAuthService;

    // Steam 로그인 URL을 생성하는 엔드포인트
    @GetMapping("/login")
    public ResponseEntity<?> getSteamLoginUrl() {
        String redirectUrl = "https://localhost:8080/oauth/steam/callback"; // 콜백 URL
        try {
            String loginUrl = steamService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("steamLoginUrl", loginUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }


    // Steam 연결 URL을 생성하는 엔드포인트
    @GetMapping("/connect")
    public ResponseEntity<?> getSteamConnectUrl() {
        try {
            String redirectUrl = "https://localhost:3000/oauth/steam/callback";
            String loginUrl = steamService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("url", loginUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }

    // Steam 콜백을 처리하는 엔드포인트
    @GetMapping("/callback")
    public void handleSteamCallback(@RequestParam("openid.claimed_id") String claimedId, @RequestParam Map<String, String> params, HttpServletResponse response) {
        try {
            if (claimedId == null || claimedId.isEmpty()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid callback parameters.");
                return;
            }
            String steamId = steamService.extractSteamId(claimedId);
            if (!steamService.validateSteamResponse(params)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Steam response validation failed");
                return;
            }
            String steamNickname = steamService.getSteamNickname(steamId);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized user");
                return;
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUsername();

            userService.linkSteamAccount(userId, steamId, steamNickname);

            String accessToken = jwtTokenProvider.generateToken(authentication).getAccessToken();
            String redirectUrl = "https://localhost:3000/oauth/steam/callback?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                    "&steamId=" + URLEncoder.encode(steamId, StandardCharsets.UTF_8) +
                    "&redirectUrl=/";  // 최종 리디렉션할 URL

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Steam authentication failed");
            } catch (IOException ioException) {
                logger.error("Redirect failed", ioException);
            }
        }
    }

    // Steam 계정을 연결하는 엔드포인트
    @PostMapping("/link")
    public ResponseEntity<?> linkSteamAccount(@RequestBody SteamLinkRequest steamLinkRequest) {
        String steamId = steamLinkRequest.getSteamId();
        String steamNickname = steamLinkRequest.getSteamNickname();
        String userId = getUserIdFromSessionOrToken(); // 현재 로그인된 사용자 ID를 가져오기

        // 사용자 정보를 DB에서 찾기
        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setSteamId(steamId); // Steam ID 설정
            user.setSteamNickname(steamNickname); // Steam 닉네임 설정

            userRepository.save(user); // 사용자 정보 업데이트

            return ResponseEntity.ok("Steam 계정이 성공적으로 연동되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다.");
        }
    }

    // 현재 로그인된 사용자 ID를 가져옴
    private String getUserIdFromSessionOrToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUsername();
        }
        throw new IllegalStateException("사용자 ID를 가져올 수 없습니다.");
    }

    // Steam 프로필을 가져오는 엔드포인트
    @GetMapping("/profile/{steamId}")    //pathvariable
    public ResponseEntity<?> getSteamProfile(@PathVariable("steamId") String steamId) {
        String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=" + steamApiKey + "&steamids=" + steamId;

        try {
            logger.info("Requesting Steam profile for SteamID: {}", steamId);
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Received Steam profile response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to fetch Steam profile for SteamID: {}", steamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Steam profile");
        }
    }
}

