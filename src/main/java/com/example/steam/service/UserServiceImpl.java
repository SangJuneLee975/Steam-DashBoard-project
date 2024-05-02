// UserServiceImpl.java
package com.example.steam.service;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.JwtToken;
import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.entity.RefreshToken;
import com.example.steam.entity.Role;
import com.example.steam.entity.SocialLogin;
import com.example.steam.model.GoogleUser;
import com.example.steam.model.NaverUser;
import com.example.steam.repository.RoleRepository;
import com.example.steam.repository.SocialLoginRepository;
import com.example.steam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RoleRepository roleRepository;
    private final SocialLoginRepository socialLoginRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           CustomUserDetailsService customUserDetailsService, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService, RoleRepository roleRepository, SocialLoginRepository socialLoginRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.roleRepository = roleRepository;
        this.socialLoginRepository = socialLoginRepository;
    }

    @Override
    public String signUp(User user, String passwordConfirm) {
        //
        Optional<User> existingUser = userRepository.findByUserId(user.getUserId());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
        }

        // 기본 권한 설정
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER"))); // ROLE_USER가 없다면 생성하여 저장
        user.addRole(defaultRole); // 사용자에게 기본 권한 부여

        // 비밀번호와 비밀번호 확인이 일치하는지 검사
        if (!user.getPassword().equals(passwordConfirm)) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        // UserDetails를 통해 Authentication 객체 생성
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUserId());
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성 및 반환 (accessToken만 반환)
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
        return jwtToken.getAccessToken();
    }

    @Override
    public boolean checkUserIdAvailable(String userId) {
        Optional<User> userOptional = userRepository.findByUserId(userId);
        return !userOptional.isPresent();
    }

    @Override
    public Map<String, String> login(String username, String password) {

        logger.info("로그인 시도: 아이디 = {}", username);
        try {
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
            String name = user.getName();

            // 응답에 액세스 토큰과 리프레시 토큰 포함
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken.getToken());
            tokens.put("nickname", nickname);


            return tokens;
        } catch (Exception e) {
            logger.error("로그인 실패: 사용자 아이디 = {}, 에러 메시지 = {}", username, e.getMessage());
            throw e;
        }
    }


    @Override
    @Transactional
    public User processGoogleUser(GoogleUser googleUser, String accessToken) {
        Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());
        User user = existingUser.orElseGet(() -> {
            // 구글 사용자 정보를 기반으로 새 사용자 생성
            User newUser = User.builder()
                    .email(googleUser.getEmail())
                    .name(googleUser.getName())
                    .userId(googleUser.getEmail()) // 이메일을 userId로 사용
                    .build();
            // 권한 설정 로직
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER"))); // ROLE_USER가 없다면 생성하여 저장
            newUser.addRole(userRole); // 생성된 사용자에 ROLE_USER 권한 부여
            return userRepository.save(newUser);
        });


        // SocialLogin Entity 생성 및 저장 로직 추가
        SocialLogin socialLogin = socialLoginRepository.findByUserAndSocialCode(user, 1) // 소셜 코드 1 = Google
                .orElseGet(() -> {
                    SocialLogin newSocialLogin = SocialLogin.builder()
                            .user(user)
                            .socialCode(1) // 소셜 코드 1 = Google
                            .externalId(googleUser.getId()) // 구글 사용자 고유 ID
                            .accessToken(accessToken) // 액세스 토큰 저장
                            .build();
                    return socialLoginRepository.save(newSocialLogin);
                });

        // 리프레시 토큰 생성 및 저장
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

        // UserDetails 생성 및 SecurityContext에 저장
        Collection<? extends GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
        UserDetails userDetails = new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                authorities
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 발급
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);

        // 소셜 로그인 정보 업데이트
        socialLogin.setAccessToken(jwtToken.getAccessToken()); // Access Token 저장
        socialLoginRepository.save(socialLogin);


        return user;
    }

}