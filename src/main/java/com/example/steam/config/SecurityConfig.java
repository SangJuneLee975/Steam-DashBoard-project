package com.example.steam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;


@Configuration
@EnableWebSecurity
public class SecurityConfig  {

    private final JwtTokenProvider jwtTokenProvider;


    @Value("${steam.client.id}")
    private String clientId;


    // OAuth2 로그인 구성을 위한 ClientRegistrationRepository 빈 등록
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(steamClient());

    }

    // Steam을 OAuth2 클라이언트로 등록하기 위한 메소드입니다.
    private ClientRegistration steamClient() {
        return ClientRegistration.withRegistrationId("steam")
                .clientId(clientId) // Steam에서 할당받은 Client ID
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                // 인증 방식 설정, Steam은 별도의 클라이언트 인증이 필요 없으므로 NONE을 지정

                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)

                .redirectUri("https://stdash.shop/login/oauth2/code/steam")
                // 인증 성공 후 리디렉트될 URI를 지정합니다.

                .authorizationUri("https://steamcommunity.com/openid/login")
                .tokenUri("https://steamcommunity.com/openid/login") //  Spring 설정을 위해 임의의 값 설정
                .userInfoUri("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/") // 사용자 정보 가져오기 URI

                .build();
    }

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(corsConfigurationSource()).and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/user/signup", "/user/checkUserId").permitAll();
                    auth.requestMatchers("/user/login").permitAll();
                    auth.requestMatchers("/oauth/steam/login/callback").permitAll(); // 로그인 콜백 허용
                    auth.requestMatchers("/oauth/steam/link/callback").permitAll(); // 계정 연동 콜백 허용
                    auth.requestMatchers("/user/**").authenticated();
                    auth.requestMatchers("/user/profile").authenticated();
                    auth.requestMatchers("/login/**").permitAll();
                    auth.requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER");
                    auth.requestMatchers("/steam/**").permitAll();
                    auth.requestMatchers("/admin/**").hasRole("ADMIN");
                    auth.anyRequest().permitAll();
                })
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }




    // PasswordEncoder Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }



    // CORS 설정을 위한 Bean 추가
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("https://stdash.shop"); // 모든 도메인 허용
        configuration.addAllowedOrigin("https://localhost:3000");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

        @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> {
                    try {
                        sslSpec.sslContext(SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)  // 모든 인증서를 신뢰
                                .build());
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                });

        // WebClient 설정
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }

}
