package com.example.steam.service;

import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.GoogleUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    public String getGoogleAuthorizationUrl() {
        String url = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=email profile" +
                "&access_type=offline";
        return url;
    }

    public OAuthTokens getAccessToken(String code) {
        // 요청을 보낼 URL
        String url = "https://oauth2.googleapis.com/token";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("code", code);
        map.add("redirect_uri", redirectUri);
        map.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // Google에서 액세스 토큰과 리프레시 토큰을 포함한 응답을 받습니다.
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        try {
        // JSON 파싱 로직
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getBody());
        String accessToken = root.path("access_token").asText();
        String refreshToken = root.path("refresh_token").asText();


            return new OAuthTokens(accessToken, refreshToken);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Google OAuth token response", e);
        }
    }

    public GoogleUser getUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<GoogleUser> response = restTemplate.getForEntity(url, GoogleUser.class);
        return response.getBody();
    }
}