package com.example.steam.service;

import com.example.steam.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String userId);

    public boolean validateRefreshToken(String token);

    String getUserIdFromRefreshToken(String token);

}
