package com.example.steam.service;

import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.NaverUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(NaverOAuthServiceImpl.class);

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

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        // API 응답을 ResponseEntity로 받아오기
        ResponseEntity<String> response = restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, entity, String.class);

        // 응답에서 JSON 데이터를 문자열로 추출
        String responseBody = response.getBody();

        // JSON 데이터를 NaverUser 객체로 파싱
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 'response' 노드에서 사용자 정보 추출
            JsonNode responseNode = rootNode.path("response");
            if (responseNode.isMissingNode()) {
                // 'response' 노드가 없다면 오류 로그 출력
                logger.error("네이버 사용자 정보 API 응답에 '응답' 노드가 누락: {}", responseBody);
                return null;
            }

            // JsonNode를 NaverUser 객체로 변환
            NaverUser naverUser = objectMapper.treeToValue(responseNode, NaverUser.class);
            return naverUser;

        } catch (JsonProcessingException e) {
            logger.error("네이버 사용자 정보 JSON을 파싱하는 중에 오류가 발생: {}", e.getMessage());
            return null;
        }
    }
}