package com.example.steam.service;

import com.example.steam.model.SteamUser;

public interface SteamService {
    SteamUser getPlayerSummaries(String steamId);
    Object getOwnedGames(String steamId);
    Object getRecentlyPlayedGames(String steamId);
}