// UserService.java
package com.example.steam.service;

import com.example.steam.dto.User;
import com.example.steam.model.GoogleUser;

import java.util.Map;

public interface UserService {
   public String signUp(User user, String passwordConfirm) ;
   public Map<String, String> login(String username, String password);

   User processGoogleUser(GoogleUser googleUser);

   boolean checkUserIdAvailable(String userId); // 중복 아이디 체크
}