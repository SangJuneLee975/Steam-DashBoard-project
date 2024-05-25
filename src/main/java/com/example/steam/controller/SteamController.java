package com.example.steam.controller;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.model.SteamUser;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.SteamAuthenticationServiceImpl;
import com.example.steam.service.SteamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/steam")
public class SteamController {
    private final SteamService steamService;
    private final SteamAuthenticationService steamAuthService;
    private static final Logger logger = LoggerFactory.getLogger(SteamController.class);

    @Autowired
    public SteamController(SteamService steamService, SteamAuthenticationService steamAuthService) {
        this.steamService = steamService;
        this.steamAuthService = steamAuthService;
    }

    @GetMapping("/profile")
    public SteamUser getSteamProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        return steamService.getPlayerSummaries(steamId);
    }

    @GetMapping("/ownedGames")
    public Object getOwnedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        return steamService.getOwnedGames(steamId);
    }

    @GetMapping("/recentlyPlayedGames")
    public Object getRecentlyPlayedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getSteamId();
        return steamService.getRecentlyPlayedGames(steamId);
    }


    @PostMapping("/link")
    public ResponseEntity<?> linkSteamAccount(@RequestParam String steamId, @RequestParam String displayName, @RequestParam String accessToken) {
        logger.info(" steamId: {}, displayName: {}, accessToken: {}", steamId, displayName, accessToken);
        if (steamId == null || displayName == null || accessToken == null) {
            return ResponseEntity.badRequest().body("파라미터 요청.");
        }
        steamAuthService.handleSteamCallback(steamId, displayName, accessToken);
        return ResponseEntity.ok().build();
    }
    

//    @PostMapping("/link")
//    public ResponseEntity<?> linkSteamAccount(@RequestParam String steamId, @RequestParam String displayName, @RequestParam String accessToken) {
//        steamAuthService.handleSteamCallback(steamId, displayName, accessToken);
//        return ResponseEntity.ok().build();
//    }
}