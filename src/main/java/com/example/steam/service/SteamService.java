package com.example.steam.service;

import com.example.steam.model.SteamUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SteamService {
    @Value("${steam.api.key}")
    private String steamApiKey;

    @Value("${steam.api.id}")
    private String steamId;


    private final RestTemplate restTemplate;

    public SteamService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SteamUser getPlayerSummaries(String steamId) {
        String url = String.format("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=%s&steamids=%s", steamApiKey, steamId);
        return restTemplate.getForObject(url, SteamUser.class);
    }

    public Object getOwnedGames(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s", steamApiKey, steamId);
        return restTemplate.getForObject(url, Object.class);
    }

    public Object getRecentlyPlayedGames(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=%s&steamid=%s", steamApiKey, steamId);
        return restTemplate.getForObject(url, Object.class);
    }
}
