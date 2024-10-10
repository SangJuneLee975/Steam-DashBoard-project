package com.example.steam.service;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.JwtToken;
import com.example.steam.entity.SocialLogin;
import com.example.steam.entity.SteamLogin;
import com.example.steam.model.SteamUser;
import com.example.steam.repository.SocialLoginRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
public class SteamAuthenticationServiceImpl implements SteamAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(SteamAuthenticationServiceImpl.class);

    @Value("${steam.api.key}")
    private String steamApiKey;

    @Value("${steam.api.id}")
    private String steamApiId;

    @Value("${steam.api.url}")
    private String steamApiUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SocialLoginRepository socialLoginRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;  // Jackson의 ObjectMapper

    // Steam OpenID 인증 URL 생성
    // 스팀 로그인 URL 생성 메소드
    @Override
    public String buildSteamLoginUrl(String redirectUrl) {
        String baseUrl = "https://steamcommunity.com/openid/login";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("openid.ns", "http://specs.openid.net/auth/2.0");
        parameters.put("openid.mode", "checkid_setup");
        parameters.put("openid.return_to", redirectUrl);
        parameters.put("openid.realm", redirectUrl);
        parameters.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
        parameters.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");

        logger.info("Steam 로그인 URL 생성: {}", baseUrl);

        return baseUrl + "?" + parameters.keySet().stream()
                .map(key -> key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    // 스팀에서 리디렉션 후 처리할 메소드
    @Override
    @Transactional
    public void processSteamUser(String steamId, String displayName) {
        logger.info("Processing Steam user: steamId={}, displayName={}", steamId, displayName);

        User user = userRepository.findByUserId(steamId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(steamId)
                            .name(displayName)
                            .isSocial(true)
                            .steamId(steamId)
                            .build();
                    userRepository.save(newUser);

                    logger.info("새로운 Steam 사용자 생성: {}", newUser);
                    return newUser;
                });

        Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findByUser(user);
        Integer socialCode = socialLoginOpt.isPresent() ? socialLoginOpt.get().getSocialCode() : null;  // socialCode가 없는 경우 null 사용


        CustomUserDetails userDetails = new CustomUserDetails(
                user.getUsername(),
                null,
                user.getName(),
                socialCode,
                steamId,
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.info("스팀 사용자 인증 설정 완료: steamId: {}", steamId);
    }


    private Authentication createAuthenticationForUser(User user) {
        // 권한을 설정하고, Authentication 객체를 생성
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    // Steam 응답을 검증하는 메서드

    @Override
    public boolean validateSteamResponse(Map<String, String> params) {
        // Steam 서버에 서명 검증 요청
        return validateSignatureWithSteam(params);
    }




    // Steam 서버에 검증 요청
    private boolean validateSignatureWithSteam(Map<String, String> params) {
        try {
            String checkAuthenticationUrl = "https://steamcommunity.com/openid/login";
            // 검증 요청 시 openid.mode를 check_authentication으로 설정
            Map<String, String> verificationParams = new HashMap<>(params);
            verificationParams.put("openid.mode", "check_authentication");

            // 파라미터를 URL 인코딩하여 요청 본문 구성
            String requestBody = verificationParams.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            // HTTP POST 요청 전송
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(checkAuthenticationUrl, entity, String.class);

            // Steam 서버 응답 확인
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                logger.info("Steam 서명 검증 응답: {}", responseBody);
                return responseBody.contains("is_valid:true");
            } else {
                logger.error("Steam 서명 검증 실패: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Steam 서명 검증 중 오류 발생", e);
            return false;
        }
    }


    // claimedId에서 Steam ID를 추출하는 메서드
    public String extractSteamId(String claimedId) {
        return claimedId.replace("https://steamcommunity.com/openid/id/", "");
    }

    // Steam ID를 기반으로 사용자를 찾거나 새로 생성하는 메서드
    public User findOrCreateSteamUser(String steamId) {
        Optional<User> existingUser = userRepository.findBySteamId(steamId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            User newUser = User.builder()
                    .userId(steamId)
                    .name("SteamUser")
                    .steamId(steamId)
                    .build();
            userRepository.save(newUser);
            return newUser;
        }
    }

    // 기존 사용자 계정에 Steam 계정을 연동하는 메서드
    @Override
    @Transactional
    public void linkSteamAccount(String userId, String steamId, String steamNickname, boolean isSteamLinked) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setSteamId(steamId);
        user.setSteamNickname(steamNickname); // 스팀 닉네임 저장
        userRepository.save(user);
    }


    // 스팀 콜백
    @Override
    @Transactional
    public void handleSteamCallback(String steamId, String displayName, String accessToken) {
        logger.info("Handling Steam callback with steamId: {}, displayName: {}", steamId, displayName);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = null;

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails currentUserDetails = (CustomUserDetails) authentication.getPrincipal();
            user = userRepository.findByUserIdOrSteamId(currentUserDetails.getUsername(), steamId).orElse(null);
            logger.info("Found user by authentication or steamId: {}", user);
        }

        if (user == null) {
            user = userRepository.findBySteamId(steamId).orElse(null);
            logger.info("Found user by steamId: {}", user);
        }

        if (user == null) {
            user = User.builder()
                    .steamId(steamId)
                    .name(displayName)
                    .userId(steamId)
                    .isSocial(true)

                    .build();
            logger.info("Created new user: {}", user);
        } else {
            user.setSteamId(steamId);
            user.setName(displayName);

            logger.info("Updated existing user: {}", user);
        }
        userRepository.save(user);
        logger.info("Saved user: {}", user);

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                user.getSocialCode(),
                user.getSteamId(),
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        logger.info("Steam user authenticated with steamId: {}", steamId);
    }


    // 닉네임 가져오기
    @Override
    public String getSteamNickname(String steamId) {
        String url = String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=%s&steamids=%s", steamApiKey, steamId);
        try {
            logger.info("Steam API 호출: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode players = root.path("response").path("players");
                if (players.isArray() && players.size() > 0) {
                    String personaname = players.get(0).path("personaname").asText();
                    logger.info("Steam nickname retrieved: {}", personaname);
                    return personaname;
                } else {
                    logger.warn("No players found in Steam response.");
                }
            } else {
                logger.error("Failed to get Steam nickname: HTTP {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to get Steam nickname", e);
        }
        return null;
    }
}