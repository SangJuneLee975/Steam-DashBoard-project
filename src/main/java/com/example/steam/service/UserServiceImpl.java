// UserServiceImpl.java
package com.example.steam.service;

import com.example.steam.DTO.JwtToken;
import com.example.steam.DTO.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.entity.RefreshToken;
import com.example.steam.model.GoogleUser;
import com.example.steam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           CustomUserDetailsService customUserDetailsService,  AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public String signUp(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        // UserDetails를 통해 Authentication 객체 생성
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUserId());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성 및 반환 (accessToken만 반환)
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
        return jwtToken.getAccessToken();
    }

    @Override
    public Map<String, String> login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 리프레시 토큰 생성 및 저장
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);

        // 액세스 토큰 생성
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
        String accessToken = jwtToken.getAccessToken();

        User user = (User) authentication.getPrincipal(); //
        logger.info("사용자 정보: 아이디 = {}, 닉네임 = {}", user.getUsername(), user.getNickname());

        String nickname = user.getNickname(); // User 객체에서 닉네임 가져오기


        // 응답에 액세스 토큰과 리프레시 토큰 포함
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken.getToken());
        tokens.put("nickname", nickname); 
        
        return tokens;
    }



    public User processGoogleUser(GoogleUser googleUser) {
        Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());
        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            // 구글 사용자 정보를 기반으로 새 사용자 생성
            User newUser = User.builder()
                    .email(googleUser.getEmail())
                    .name(googleUser.getName())
                    // 기타 필요한 정보 설정
                    .build();
            return userRepository.save(newUser);
        }
    }
}