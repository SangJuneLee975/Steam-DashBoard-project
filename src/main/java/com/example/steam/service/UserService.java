// UserService.java
package com.example.steam.service;

import com.example.steam.DTO.User;
import com.example.steam.model.GoogleUser;

import java.util.List;
import java.util.Map;

public interface UserService {
   String signUp(User user);
   public Map<String, String> login(String username, String password);

   User processGoogleUser(GoogleUser googleUser);
}