package com.example.steam.controller;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.CustomUserDetailsService;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.SteamService;
import com.example.steam.model.SteamUser;
import com.example.steam.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/steam")
public class SteamController {

    private static final Logger logger = LoggerFactory.getLogger(SteamController.class);

    @Value("${steam.api.key}")
    private String steamApiKey;

    @Autowired
    private SteamService steamService;

    private final RestTemplate restTemplate;

    @Autowired
    public SteamController(RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }


    // 모든 게임 데이터를 수집하는 엔드포인트
    @GetMapping("/allGameStats")
    public Object getAllGameStats(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername();
        return steamService.getAllGameStats(steamId);
    }

    // 스팀 프로필을 가져오는 메소드
    @GetMapping("/profile")
    public SteamUser getSteamProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername();
        return steamService.getPlayerSummaries(steamId);
    }

    // 소유한 게임들을 가져오는 엔드포인트
    @GetMapping("/ownedGames")
    public ResponseEntity<?> getOwnedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        try {
            Object games = steamService.getOwnedGames(steamId);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching games");
        }
    }

    // 최근 플레이한 게임들을 가져오는 엔드포인트
    @GetMapping("/recentlyPlayedGames")
    public ResponseEntity<?> getRecentlyPlayedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        try {
            Map<String, Object> games = steamService.getRecentlyPlayedGames(steamId);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching games");
        }
    }

    // 스팀 커뮤니티 페이지에서 AppID기준으로 리뷰들을 가져오는 엔드포인트
    @GetMapping("/reviews")
    public ResponseEntity<List<String>> getReviews(@RequestParam("appId") String appId) {
        try {
            List<String> reviews = steamService.getReviews(appId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            logger.error("appid리뷰를 가져오는 중에 오류가 발생: " + appId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    //
    // 소유한 게임 수를 반환하는 엔드포인트 추가
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/ownedGamesCount")
    public ResponseEntity<Integer> getOwnedGamesCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        try {
            int gameCount = steamService.getOwnedGamesCount(steamId);
            return ResponseEntity.ok(gameCount);
        } catch (Exception e) {
            logger.error("Error fetching owned games count for user: " + steamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 최근 2주 동안 플레이한 게임 수
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/recentlyPlayedGamesCount")
    public ResponseEntity<Integer> getRecentlyPlayedGamesCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        try {
            int recentGameCount = steamService.getRecentlyPlayedGamesCount(steamId);
            return ResponseEntity.ok(recentGameCount);
        } catch (Exception e) {
            logger.error("Error fetching recently played games count for user: " + steamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // appid에 대한 업적 데이터
    @GetMapping("/globalAchievements")
    public ResponseEntity<?> getGlobalAchievements(@RequestParam("gameid") String gameid) {
        // 해당 로직을 구현합니다.
        try {
            // 예시: 해당 gameid에 대한 업적 데이터를 가져오는 로직 구현
            Map<String, Object> achievements = steamService.getGlobalAchievements(gameid);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching global achievements");
        }
    }

    // 스팀 프로필 정보
    @GetMapping("/getPlayerSummaries")
    public ResponseEntity<?> getPlayerSummaries(@RequestParam("steamId") String steamId) {
        try {
            SteamUser user = steamService.getPlayerSummaries(steamId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error fetching player summaries for user: " + steamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 스팀 Appid로 현재 플레이 하고 있는 수
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/currentPlayers")
    public ResponseEntity<?> getCurrentPlayers(@RequestParam("appid") String appid) {
        try {
            int playerCount = steamService.getCurrentPlayers(appid);
            Map<String, Integer> response = new HashMap<>();
            response.put("player_count", playerCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching current players for appid: " + appid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 스팀 프로필 정보
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/playerSummary")
    public ResponseEntity<?> getPlayerSummary(@RequestParam("steamId") String steamId) {
        try {
            SteamUser user = steamService.getPlayerSummaries(steamId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error fetching player summaries for user: " + steamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching player summary");
        }
    }

    // 스팀 프로필 정보 엔드포인트
    @GetMapping("/steamProfile")
    public ResponseEntity<?> getSteamProfile(@RequestParam("steamId") String steamId) {
        try {
            SteamUser user = steamService.getSteamProfile(steamId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error fetching steam profile for user: " + steamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching steam profile");
        }
    }
}
