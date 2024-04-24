package com.example.steam.service;

import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.NaverUser;

public interface NaverOAuthService {
    String getNaverAuthorizationUrl();
    OAuthTokens getAccessToken(String code, String state);
    NaverUser getUserInfo(String accessToken);
}