package com.example.steam.controller;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.service.SteamService;
import com.example.steam.model.SteamUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/steam")
public class SteamController {
    @Autowired
    private SteamService steamService;

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
    public Object getOwnedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername();
        return steamService.getOwnedGames(steamId);
    }

    @GetMapping("/recentlyPlayedGames")
    public Object getRecentlyPlayedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername();
        return steamService.getRecentlyPlayedGames(steamId);
    }
}