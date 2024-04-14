package com.example.steam.service;

import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.GoogleUser;
import java.io.IOException;


public interface GoogleOAuthService {

    String getGoogleAuthorizationUrl();

    OAuthTokens getAccessToken(String code) throws IOException;

    GoogleUser getUserInfo(String accessToken);
}