package com.example.steam.config;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.JwtToken;
import com.example.steam.dto.User;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.RefreshTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key key;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;


    // application.yaml에서 secret 값 가져와서 key에 저장
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, RefreshTokenService refreshTokenService,UserRepository userRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    // 사용자 정보를 가지고 AccessToken, RefreshToken을 생성하는 메서드
    public JwtToken generateToken(Authentication authentication) {


        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        String encodedName = URLEncoder.encode(userDetails.getName(), StandardCharsets.UTF_8); // 이름 인코딩

        long now = System.currentTimeMillis();
        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + 86400000);  // 1 day



        String accessToken = Jwts.builder()
                .setSubject(username)
                .claim("auth", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))
                .claim("name", encodedName)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new JwtToken("Bearer", accessToken, refreshToken);

//        return JwtToken.builder()
//                .grantType("Bearer")
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .build();
    }

    // Jwt 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // Jwt 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        String userId = claims.getSubject();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userId));
        String decodedName = URLDecoder.decode(claims.get("name", String.class), StandardCharsets.UTF_8);


        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication return
        // UserDetails: interface, User: UserDetails를 구현한 class
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                authorities
        );

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    // 토큰 정보를 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty.", e);
        }
        return false;
    }


    // accessToken
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // Request에서 토큰 추출 메서드 추가
    public String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 액세스 토큰
    public String generateAccessToken(String username, Collection<? extends GrantedAuthority> authorities) {
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + 86400000); //   24시간 후 만료 설정

        return Jwts.builder()
                .setSubject(username)
                .claim("auth", authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))


                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 리프레시 토큰 검증 메서드
    public boolean validateRefreshToken(String token) {

        return refreshTokenService.validateRefreshToken(token); 
    }
}