package com.example.steam.service;

import com.example.steam.dto.User;
import org.springframework.security.core.Authentication;

public interface SteamAuthenticationService {
   public String buildSteamLoginUrl(String redirectUrl);
   public void processSteamUser(String steamId, String displayName);


   public boolean validateSessionTicket(String sessionTicket, String steamId, String expectedSteamId);
}
