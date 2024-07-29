package com.example.steam.service;

import com.example.steam.model.SteamUser;

import java.util.List;
import java.util.Map;

public interface SteamService {
    SteamUser getPlayerSummaries(String steamId);
    Object getOwnedGames(String steamId);
    Map<String, Object> getRecentlyPlayedGames(String steamId);

    Object getAllGameStats(String steamId); // 모든 게임을 수집하는 메서드

    List<String> getReviews(String appId);  // 스팀 게임의 리뷰를 크롤링

    Map<String, String> getOwnedGamesList(String steamId); // 사용자가 소유한 게임 목록 가져오기

    int getOwnedGamesCount(String steamId);

    int getRecentlyPlayedGamesCount(String steamId);

    public int getCurrentPlayers(String appId);

    Map<String, Object> getGlobalAchievements(String gameid);

    public SteamUser getSteamProfile(String steamId); //스팀 프로필 정보

}