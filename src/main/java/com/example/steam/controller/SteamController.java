package com.example.steam.controller;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.CustomUserDetailsService;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.SteamService;
import com.example.steam.model.SteamUser;
import com.example.steam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/steam")
public class SteamController {

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

    @GetMapping("/profile")
    public SteamUser getSteamProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername();
        return steamService.getPlayerSummaries(steamId);
    }

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


    @GetMapping("/recentlyPlayedGames")
    public Object getRecentlyPlayedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername();
        try {
            Object games = steamService.getRecentlyPlayedGames(steamId);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching games");
        }
    }
}