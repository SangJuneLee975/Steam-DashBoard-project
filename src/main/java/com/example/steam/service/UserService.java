// UserService.java
package com.example.steam.service;

import com.example.steam.dto.User;
import com.example.steam.model.GoogleUser;
import com.example.steam.model.NaverUser;

import java.util.Map;
import java.util.Optional;

public interface UserService {
   public String signUp(User user, String passwordConfirm) ;
   public Map<String, String> login(String username, String password);

   public User processGoogleUser(GoogleUser googleUser, String accessToken);

  // User processNaverUser(NaverUser naverUser, String accessToken);

   boolean checkUserIdAvailable(String userId); // 중복 아이디 체크

   public User updateUserProfile(String username, Map<String, String> updates);

   public User findOrCreateSteamUser(String steamId);

   public boolean isSteamLinked(String userId);

   public Optional<User> getUserWithSteamInfo(String userId);
}