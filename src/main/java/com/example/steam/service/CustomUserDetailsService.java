package com.example.steam.service;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.entity.Role;
import com.example.steam.entity.SocialLogin;
import com.example.steam.repository.SocialLoginRepository;
import com.example.steam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository,SocialLoginRepository socialLoginRepository ) {
        this.userRepository = userRepository;
        this.socialLoginRepository = socialLoginRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // SocialLogin에서 socialCode 가져오기
        Optional<SocialLogin> socialLoginOptional = socialLoginRepository.findByUser(user);
        socialLoginOptional.ifPresent(socialLogin -> user.setSocialCode(socialLogin.getSocialCode()));

        Integer socialCode = user.getSocialCode();

        return new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                user.getSocialCode(),
                mapRolesToAuthorities(user.getRoles())  // 권한 설정
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }
}