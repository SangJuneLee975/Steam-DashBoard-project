package com.example.steam.controller;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.repository.UserRepository;
import com.example.steam.service.RefreshTokenService;
import com.example.steam.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    @Autowired
    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService,UserRepository userRepository,ObjectMapper objectMapper) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }


    // 회원가입 엔드포인트
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user) {
        try {
            // 여기서 user 객체는 클라이언트에서 보낸 JSON이 User 클래스로 변환되어 입력됩니다.
            // 사용자 등록 로직 실행
            String token = userService.signUp(user, user.getPasswordConfirm());

            // 응답 생성
            Map<String, String> response = new HashMap<>();
            response.put("message", "회원가입 성공");
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 에러 처리 로직
            logger.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원가입 실패");
        }
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
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 사용자 정보 가져오기
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername(); // 사용자 ID
            String name = userDetails.getName(); // 사용자 이름

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication).getAccessToken();
            String refreshToken = refreshTokenService.createRefreshToken(userCredentials.get("username")).getToken();



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

    // 사용자 정보를 반환하는 API
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findByUserId(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
            String jsonResponse = objectMapper.writeValueAsString(user);
            return ResponseEntity.ok().body(jsonResponse);
        } catch (JsonProcessingException e) {
            logger.error("JSON writing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing JSON");
        } catch (Exception e) {
            logger.error("Error retrieving user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody Map<String, String> updates) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userService.updateUserProfile(userDetails.getUsername(), updates);

            return ResponseEntity.ok().body(user);
        } catch (Exception e) {
            logger.error("프로필 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("프로필 업데이트 중 오류 발생");
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