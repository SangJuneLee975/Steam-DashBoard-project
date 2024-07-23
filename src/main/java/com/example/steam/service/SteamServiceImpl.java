package com.example.steam.service;

import com.example.steam.model.SteamUser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;

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
    public Map<String, Object> getRecentlyPlayedGames(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=%s&steamid=%s", steamApiKey, steamId);
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException e) {
            // 에러 로그 추가
            System.out.println("Error: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    // 모든 게임 데이터를 수집하는 메서드
    @Override
    public Object getAllGameStats(String steamId) {
        String url = String.format("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=true&include_played_free_games=true", steamApiKey, steamId);
        return restTemplate.getForObject(url, Object.class);
    }

    // 특정 게임의 리뷰를 가져오는 메서드
    @Override
    public List<String> getReviews(String appId) {
        List<String> reviews = new ArrayList<>();
        try {
            String url = "https://steamcommunity.com/app/" + appId + "/reviews/?browsefilter=toprated";
            Document doc = Jsoup.connect(url).get();
            Elements reviewElements = doc.select(".apphub_CardTextContent");
            for (Element reviewElement : reviewElements) {
                reviews.add(reviewElement.text());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    // 소유한 게임 목록을 가져오는 메서드
    @Override
    public Map<String, String> getOwnedGamesList(String steamId) {
        Map<String, String> gamesList = new HashMap<>();
        String url = String.format("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=true", steamApiKey, steamId);
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> games = (List<Map<String, Object>>) ((Map<String, Object>) response.get("response")).get("games");
            for (Map<String, Object> game : games) {
                String appId = String.valueOf(game.get("appid"));
                String name = (String) game.get("name");
                gamesList.put(appId, name);
            }
        } catch (HttpClientErrorException e) {
            System.out.println("Error: " + e.getResponseBodyAsString());
            throw e;
        }
        return gamesList;
    }


}