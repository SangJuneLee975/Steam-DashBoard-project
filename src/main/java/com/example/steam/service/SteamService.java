package com.example.steam.service;

import com.example.steam.model.SteamUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import com.example.steam.model.SteamUser;

public interface SteamService {
    SteamUser getPlayerSummaries(String steamId);
    Object getOwnedGames(String steamId);
    Object getRecentlyPlayedGames(String steamId);
}