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
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RequestMapping("/oauth/steam")
@RestController
public class SteamOAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SteamOAuthController.class);

    @Value("${steam.api.key}")
    private String steamApiKey;   // steam api key

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
        String redirectUrl = "https://localhost:8080/oauth/steam/login/callback"; // 로그인 콜백 URL
        //  String redirectUrl = "https://stdash.shop/oauth/steam/login/callback"; // 로그인 콜백 URL
        try {
            String loginUrl = steamService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("steamLoginUrl", loginUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }


    // Steam 연결 URL을 생성하는 엔드포인트
    @GetMapping("/connect")
    public ResponseEntity<?> getSteamConnectUrl(@RequestHeader("Authorization") String accessToken) {
        logger.info("Received accessToken: {}", accessToken); // 로그로 accessToken 확인
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing accessToken");
        }
        try {

            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7); // 'Bearer ' 제거
            }
            String redirectUrl = "https://localhost:8080/oauth/steam/link/callback?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            //String redirectUrl = "https://stdash.shop/oauth/steam/link/callback?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            String loginUrl = steamService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("url", loginUrl));
        } catch (Exception e) {
            logger.error("Steam 로그인 URL 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }

    // Steam 콜백을 처리하는 엔드포인트
    @GetMapping("/callback")
    public void handleSteamCallback(@RequestParam("openid.claimed_id") String claimedId, @RequestParam Map<String, String> params, HttpServletResponse response) {
        try {

            // 로깅 추가: 스팀에서 반환된 파라미터와 클레임 ID 확인
            logger.info("Steam OpenID Callback Params: {}", params);
            logger.info("Steam OpenID Claimed ID: {}", claimedId);



            if (claimedId == null || claimedId.isEmpty()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid callback parameters.");
                return;

            }

            String steamId = steamService.extractSteamId(claimedId);
            // 서명 검증
            if (!steamService.validateSteamResponse(params)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Steam response validation failed");
                return;
            }
            String steamNickname = steamService.getSteamNickname(steamId);

            // 인증된 사용자 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized user");
                return;
            }

            // 사용자 정보를 업데이트
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUsername();
            userService.linkSteamAccount(userId, steamId, steamNickname);

            String accessToken = jwtTokenProvider.generateToken(authentication).getAccessToken();
            String redirectUrl = "https://localhost:8080/profile?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            // String redirectUrl = "https://stdash.shop/profile?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);


//            String redirectUrl = "https://stdash.shop/oauth/steam/callback?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
//                    "&steamId=" + URLEncoder.encode(steamId, StandardCharsets.UTF_8) +
//                    "&redirectUrl=/";  // 최종 리디렉션할 URL


            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Steam authentication failed");
            } catch (IOException ioException) {
                logger.error("Redirect failed", ioException);
            }
        }
    }

    // Steam 로그인 콜백을 처리하는 엔드포인트
    @GetMapping("/login/callback")
    public ResponseEntity<?> handleSteamLoginCallback(@RequestParam Map<String, String> params, HttpServletResponse response) {
        try {
            logger.info("Steam OpenID Login Callback Params: {}", params);

            // 서명 검증
            boolean isValid = steamAuthService.validateSteamResponse(params);
            if (!isValid) {
                logger.error("Steam 서명 검증 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Steam authentication");
            }

            // Steam ID 추출
            String claimedId = params.get("openid.claimed_id");
            String steamId = steamAuthService.extractSteamId(claimedId);
            logger.info("Steam OpenID Claimed ID: {}", claimedId);

            // Steam 닉네임 가져오기
            String steamNickname = steamAuthService.getSteamNickname(steamId);
            logger.info("Steam 닉네임: {}", steamNickname);

            // 사용자 처리 (로그인)
            steamAuthService.processSteamUser(steamId, steamNickname);

            // JWT 토큰 생성
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String accessToken = jwtTokenProvider.generateToken(authentication).getAccessToken();

            // 프론트엔드로 리디렉션 (토큰 포함)
            String redirectUrl = "https://stdash.shop/login?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);

            return ResponseEntity.ok("Steam authentication successful");
        } catch (Exception e) {
            logger.error("Steam 로그인 콜백 처리 중 오류 발생", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Steam authentication failed");
            } catch (IOException ioException) {
                logger.error("Redirect 실패", ioException);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam authentication failed");
        }
    }

    // 계정 연동 콜백을 처리하는 엔드포인트
    @GetMapping("/link/callback")
    public void handleSteamLinkCallback(@RequestParam("openid.claimed_id") String claimedId,
                                        @RequestParam Map<String, String> params,
                                        HttpServletResponse response) {
        try {
            logger.info("Steam OpenID Link Callback Params: {}", params);
            logger.info("Steam OpenID Claimed ID: {}", claimedId);

            if (claimedId == null || claimedId.isEmpty()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid callback parameters.");
                return;
            }

            String steamId = steamAuthService.extractSteamId(claimedId);

            // 서명 검증
            if (!steamAuthService.validateSteamResponse(params)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Steam response validation failed");
                return;
            }

            String steamNickname = steamAuthService.getSteamNickname(steamId);

            // 'openid.return_to'에서 accessToken 추출
            String returnToUrl = params.get("openid.return_to");
            if (returnToUrl == null || returnToUrl.isEmpty()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing openid.return_to parameter");
                return;
            }

            URI returnUri = new URI(returnToUrl);
            String query = returnUri.getQuery();
            if (query == null) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing query parameters in return_to URL");
                return;
            }
            Map<String, String> returnParams = Arrays.stream(query.split("&"))
                    .map(param -> param.split("="))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(arr -> arr[0], arr -> URLDecoder.decode(arr[1], StandardCharsets.UTF_8)));

            String accessToken = returnParams.get("accessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing accessToken in return_to URL");
                return;
            }

            // JWT 토큰 유효성 검사
            if (!jwtTokenProvider.validateToken(accessToken)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid accessToken");
                return;
            }

            // Authentication 객체 설정
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 사용자 정보 추출
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUsername();

            // Steam 계정 연동
            userService.linkSteamAccount(userId, steamId, steamNickname);

            // 새로운 AccessToken 생성 (필요 시)
            String newAccessToken = jwtTokenProvider.generateToken(authentication).getAccessToken();

            // 프론트엔드로 리디렉션 (토큰 포함)
            String redirectUrl = "https://localhost:8080/profile?accessToken=" + URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8);
            //String redirectUrl = "https://stdash.shop/profile?accessToken=" + URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            logger.error("Steam authentication failed", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Steam authentication failed");
            } catch (IOException ioException) {
                logger.error("Redirect failed", ioException);
            }
        }
    }


    // Steam 계정을 연결하는    엔드포인트
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

    // 로그인된 사용자 ID를 가져오는 메소드
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

