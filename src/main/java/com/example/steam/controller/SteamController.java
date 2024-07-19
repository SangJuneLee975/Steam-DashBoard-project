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

    // Steam 프로필을 가져오는 엔드포인트
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

    // 최근에 플레이한 게임들을 가져오는 엔드포인트
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

}