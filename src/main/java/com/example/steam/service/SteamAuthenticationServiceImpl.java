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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


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
                    User newUser = new User(steamId, null, displayName, null, null, true);
                    userRepository.save(newUser);
                    return newUser;
                });

        Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findByUser(user);
        Integer socialCode = socialLoginOpt.isPresent() ? socialLoginOpt.get().getSocialCode() : null;  // socialCode가 없는 경우 null 사용


        CustomUserDetails userDetails = new CustomUserDetails(
                user.getUsername(),
                null,
                user.getName(),
                socialCode,
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

    @Override
    public boolean validateSessionTicket(String sessionTicket, String steamId, String expectedSteamId) {   // expectedSteamId 스팀아이디 검증 
        final String url = "https://partner.steam-api.com/ISteamUserAuth/AuthenticateUserTicket/v1/";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> params = new HashMap<>();
        params.put("key", steamApiKey);
        params.put("appid", steamApiId);
        params.put("ticket", sessionTicket); // 세션 티켓을 16진수로 인코딩된 문자열로 변환

        // 파라미터를 URL에 쿼리 스트링으로 추가
        String paramsAsString = params.keySet().stream()
                .map(key -> key + "=" + params.get(key))
                .collect(Collectors.joining("&"));

        String fullUrl = url + "?" + paramsAsString;

        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.error("스팀 티켓 검증 실패: HTTP 오류");
            return false;
        }

        try {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode steamIdNode = jsonResponse.path("response").path("steamid");

            if (steamIdNode.isMissingNode()) {
                logger.error("응답에서 SteamID를 찾을 수 없습니다.");
                return false;
            }

            String returnedSteamId = steamIdNode.asText();
            if (returnedSteamId.equals(expectedSteamId)) {
                logger.info("SteamID 검증 성공: {}", returnedSteamId);
                return true;
            } else {
                logger.error("SteamID 검증 실패: 예상된 SteamID {}, 받은 SteamID {}", expectedSteamId, returnedSteamId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Steam API로부터 JSON 응답 파싱 중 오류 발생", e);
            return false;
        }
    }

    // Steam 응답을 검증하는 메서드
    public boolean validateSteamResponse(Map<String, String> params) {
        // Steam 응답을 검증하는 로직 추가
        return true;
    }

    // claimedId에서 Steam ID를 추출하는 메서드
    public String extractSteamId(String claimedId) {
        // claimedId에서 Steam ID를 추출
        return claimedId.replace("https://steamcommunity.com/openid/id/", "");
    }

    // Steam ID를 기반으로 사용자를 찾거나 새로 생성하는 메서드
    public User findOrCreateSteamUser(String steamId) {
        // 중복 체크 및 업데이트
        Optional<User> existingUser = userRepository.findBySteamId(steamId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            User newUser = User.builder()
                    .userId(steamId)
                    .name("SteamUser")  // 임시로 SteamUser로 지정
                    .build();
            userRepository.save(newUser);
            return newUser;
        }
    }

    // 기존 사용자 계정에 Steam 계정을 연동하는 메서드
    public void linkSteamAccount(User user, String steamId) {
        // 기존 사용자 계정에 Steam 계정을 연동
        user.setSteamId(steamId);
        userRepository.save(user);
    }

    @Override
    public String getSteamNickname(String steamId) {
        String url = String.format("%s?key=%s&steamids=%s", steamApiUrl, steamApiKey, steamId);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode players = root.path("response").path("players");
                if (players.isArray() && players.size() > 0) {
                    return players.get(0).path("personaname").asText();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get Steam nickname", e);
        }
        return null;
    }
}