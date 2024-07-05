package com.example.steam.service;

import com.example.steam.model.SteamUser;

public interface SteamService {
    SteamUser getPlayerSummaries(String steamId);
    Object getOwnedGames(String steamId);
    Object getRecentlyPlayedGames(String steamId);

    Object getAllGameStats(String steamId); // 모든 게임을 수집하는 메서드
}