package com.example.steam.service;

import com.example.steam.model.SteamUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class SteamServiceImpl implements SteamService {
    @Value("${steam.api.key}")
    private String steamApiKey;

    private final RestTemplate restTemplate;

    public SteamServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public SteamUser getPlayerSummaries(String steamId) {
        String url = String.format("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=%s&steamids=%s", steamApiKey, steamId);
        return restTemplate.getForObject(url, SteamUser.class);
    }

    @Override
    public Object getOwnedGames(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=true", steamApiKey, steamId);
        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (HttpClientErrorException e) {
            // 에러 로그 추가
            System.out.println("Error: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @Override
    public Object getRecentlyPlayedGames(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=%s&steamid=%s", steamApiKey, steamId);
        return restTemplate.getForObject(url, Object.class);
    }

    // 모든 게임 데이터를 수집하는 메서드
    @Override
    public Object getAllGameStats(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=true&include_played_free_games=true", steamApiKey, steamId);
        return restTemplate.getForObject(url, Object.class);
    }
}