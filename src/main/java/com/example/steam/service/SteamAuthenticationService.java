package com.example.steam.service;

import com.example.steam.dto.User;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface SteamAuthenticationService {
   public String buildSteamLoginUrl(String redirectUrl);
   public void processSteamUser(String steamId, String displayName);


   public boolean validateSessionTicket(String sessionTicket, String steamId, String expectedSteamId);

   public boolean validateSteamResponse(Map<String, String> params); // Steam 응답을 검증하는 메서드

   public String extractSteamId(String claimedId); // claimedId에서 Steam ID를 추출하는 메서드

   public User findOrCreateSteamUser(String steamId); // Steam ID를 기반으로 사용자를 찾거나 새로 생성하는 메서드

   public void linkSteamAccount(User user, String steamId);   // 기존 사용자 계정에 Steam 계정을 연동하는 메서드

   public String getSteamNickname(String steamId); // Steam ID를 통해 프로필 이름을 가져오는 메서드

   public void handleSteamCallback(String steamId, String displayName, String accessToken);
}
