package com.example.steam.controller;

import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.RefreshTokenService;
import com.example.steam.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    @Autowired
    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService,UserRepository userRepository) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    // 회원가입 엔드포인트
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user) {
        String token = userService.signUp(user, user.getPasswordConfirm());
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원가입 성공");
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkUserId")
    public ResponseEntity<?> checkUserId(@RequestParam("userId") String userId) {
        boolean isAvailable = userService.checkUserIdAvailable(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isAvailable", isAvailable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> userCredentials) {
        logger.info("로그인 시도: 사용자 아이디 = {}", userCredentials.get("username"));
        try {
            logger.info("인증 시도 전: {}", userCredentials.get("username"));

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userCredentials.get("username"), userCredentials.get("password"));

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 인증 시도 후 로그
            logger.info("인증 성공: 사용자 아이디 = {}", userCredentials.get("username"));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtTokenProvider.generateToken(authentication).getAccessToken();
            String refreshToken = refreshTokenService.createRefreshToken(userCredentials.get("username")).getToken();

            // 로그인 성공 로그
            logger.info("로그인 성공: 사용자 아이디 = {}", userCredentials.get("username"));

            Map<String, String> response = new HashMap<>();
            response.put("message", "로그인 성공");
            response.put("accessToken", jwt);
            response.put("refreshToken", refreshToken);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 로그인 실패 로그
            logger.error("로그인 실패: 사용자 아이디 = {}, 에러 메시지 = {}", userCredentials.get("username"), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }



    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestParam String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid Refresh Token");
        }

        // 리프레시 토큰으로부터 userId 추출
        String userId = refreshTokenService.getUserIdFromRefreshToken(refreshToken);
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        // 새로운 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, authorities);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);

        return ResponseEntity.ok(tokens);
    }
}