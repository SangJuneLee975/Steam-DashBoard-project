package com.example.steam.entity;

import lombok.Value;

@Value
public class OAuthTokens {
    String accessToken;
    String refreshToken;
}
