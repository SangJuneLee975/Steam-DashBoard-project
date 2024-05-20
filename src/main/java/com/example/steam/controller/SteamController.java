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

    @GetMapping("/profile")
    public SteamUser getSteamProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername(); // 사용자 Steam ID 가져오기
        return steamService.getPlayerSummaries(steamId);
    }

    @GetMapping("/ownedGames")
    public Object getOwnedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername(); // 사용자 Steam ID 가져오기
        return steamService.getOwnedGames(steamId);
    }

    @GetMapping("/recentlyPlayedGames")
    public Object getRecentlyPlayedGames(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String steamId = userDetails.getUsername(); // 사용자 Steam ID 가져오기
        return steamService.getRecentlyPlayedGames(steamId);
    }
}