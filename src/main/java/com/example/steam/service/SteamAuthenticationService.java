package com.example.steam.service;

public interface SteamAuthenticationService {
   public String buildSteamLoginUrl(String redirectUrl);
   public void processSteamUser(String steamId, String displayName);

}
