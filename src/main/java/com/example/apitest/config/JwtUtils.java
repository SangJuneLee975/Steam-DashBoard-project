package com.example.apitest.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 토큰 유효기간 1일

    private static final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512); //  JWT 서명에 사용할 키를 생성


    // JWT 생성
    public String generateToken(String userId, String name, String nickname) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(userId) // 사용자 ID를 subject로 설정
                .claim("name", name)
                .claim("nickname", nickname)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key)
                .compact();
    }

    // --JWT의 유효성을 검증하고, 토큰에서 정보를 추출하는데 사용하는 메서드들--

    // JWT에서 사용자 ID 추출
    public String extractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // JWT에서 만료일 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // JWT에서 클레임 추출
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // JWT에서 모든 클레임 추출
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody(); // 수정된 부분
    }

    // JWT 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token); // 수정된 부분
            return true;
        } catch (Exception e) {
            System.out.println("Invalid token: " + e.getMessage());
            return false;
        }
    }
    // 요청에서 JWT 추출
    public String extractToken(HttpServletRequest request) {
        // 요청 헤더에서 토큰을 추출하는 로직을 추가할 수 있습니다.
        // 여기서는 간단한 예시로 Authorization 헤더에서 Bearer 토큰을 추출하도록 작성합니다.
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
