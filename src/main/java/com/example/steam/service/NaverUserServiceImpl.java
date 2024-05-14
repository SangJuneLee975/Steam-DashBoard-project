package com.example.steam.service;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.entity.RefreshToken;
import com.example.steam.entity.Role;
import com.example.steam.entity.SocialLogin;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;


@Service
public class NaverUserServiceImpl implements NaverUserService {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    private static final Logger logger = LoggerFactory.getLogger(NaverUserServiceImpl.class);
    @Autowired
    public NaverUserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                                SocialLoginRepository socialLoginRepository, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.socialLoginRepository = socialLoginRepository;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public User processNaverUser(NaverUser naverUser, String accessToken) {

        if (naverUser == null) {

            throw new IllegalArgumentException("Received null Naver user data");
        }
        logger.info("Received Naver user data: {}", naverUser);
        // 사용자 이메일을 기준으로 기존 사용자 검색 또는 새 사용자 등록
        Optional<User> existingUser = userRepository.findByEmail(naverUser.getEmail());
        User user = existingUser.orElseGet(() -> {

                    // 새로운 User 객체 생성
        User newUser = User.builder()
                      .email(naverUser.getEmail())
                      .name(naverUser.getName())
                      .userId(naverUser.getEmail()) // 이메일을 userId로 사용
                      .nickname(naverUser.getNickname())
                      .isSocial(true)
                      .build();

                    // 기본 권한 설정
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
                    newUser.addRole(userRole);

                    // 새 사용자 저장

                    return userRepository.save(newUser);
                });




        // 소셜 로그인 정보 업데이트 또는 새로 생성
        SocialLogin socialLogin = socialLoginRepository.findByUserAndSocialCode(user, 2) // 소셜 코드 2 = Naver
                .orElseGet(() -> {
                    SocialLogin newSocialLogin = SocialLogin.builder()
                            .user(user)
                            .socialCode(2)
                            .externalId(naverUser.getId())
                            .accessToken(accessToken)
                            .build();
                    return socialLoginRepository.save(newSocialLogin);
                });

        // 액세스 토큰 업데이트
        socialLogin.setAccessToken(accessToken);
        socialLoginRepository.save(socialLogin);


        // 리프레시 토큰 생성 및 저장
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

        Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findByUser(user);
        Integer socialCode = socialLoginOpt.isPresent() ? socialLoginOpt.get().getSocialCode() : null;  // socialCode가 없는 경우 null 사용


        // JWT 토큰 발급 및 SecurityContext에 인증 정보 등록
        Collection<? extends GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
        UserDetails userDetails = new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                socialCode,
                authorities
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return user;
    }
}
