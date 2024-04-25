package com.example.steam.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class OAuthTokens {
    String accessToken;
    String refreshToken;

    @JsonCreator
    public OAuthTokens(@JsonProperty("access_token") String accessToken,
                       @JsonProperty("refresh_token") String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
