package com.example.steam.service;

import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.NaverUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class NaverOAuthServiceImpl implements NaverOAuthService {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    @Value("${naver.redirect.uri}")
    private String redirectUri;

    private static final String AUTH_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    @Override
    public String getNaverAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        return AUTH_URL + "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&state=" + state;
    }

    @Override
    public OAuthTokens getAccessToken(String code, String state) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("state", state);
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<OAuthTokens> response = restTemplate.postForEntity(TOKEN_URL, request, OAuthTokens.class);

        return response.getBody();
    }

    @Override
    public NaverUser getUserInfo(String accessToken) {
        // 사용자 정보 조회 로직
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<NaverUser> response = restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, entity, NaverUser.class);
        return response.getBody();
    }
}