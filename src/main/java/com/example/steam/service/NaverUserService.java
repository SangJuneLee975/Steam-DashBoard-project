package com.example.steam.service;

import com.example.steam.dto.User;
import com.example.steam.model.NaverUser;

public interface NaverUserService {
    User processNaverUser(NaverUser naverUser, String accessToken);
}