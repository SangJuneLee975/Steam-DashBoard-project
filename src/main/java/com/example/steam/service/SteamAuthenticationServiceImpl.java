package com.example.steam.service;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.JwtToken;
import com.example.steam.entity.SteamLogin;
import com.example.steam.model.SteamUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;

@Service
public class SteamAuthenticationServiceImpl implements SteamAuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Steam OpenID 인증 URL 생성
    // 스팀 로그인 URL 생성 메소드
    public String buildSteamLoginUrl(String redirectUrl) {
        return "https://steamcommunity.com/openid/login?" +
                "openid.ns=http://specs.openid.net/auth/2.0" +
                "&openid.mode=checkid_setup" +
                "&openid.return_to=" + redirectUrl +
                "&openid.realm=" + redirectUrl +
                "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select" +
                "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select";
    }

    // 스팀에서 리디렉션 후 처리할 메소드
    @Transactional
    public void processSteamUser(String steamId, String displayName) {
        User user = userRepository.findByUserId(steamId)
                .orElseGet(() -> User.builder()
                        .userId(steamId)
                        .name(displayName)
                        .build());

        SteamLogin steamLogin = SteamLogin.builder()
                .steamId(steamId)
                .user(user)
                .build();
        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user.getUserId(), null, user.getName(), AuthorityUtils.createAuthorityList("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
        // JWT 토큰 사용 또는 반환 등의 추가적인 로직 구현
    }

    private Authentication createAuthenticationForUser(User user) {
        // 권한을 설정하고, Authentication 객체를 생성
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}