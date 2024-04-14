package com.example.steam.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthenticationInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 요청에서 JWT 토큰을 추출
        String token = jwtTokenProvider.extractToken(request);


        System.out.println("요청 URL: " + request.getRequestURL());
        System.out.println("토큰 추출: " + token);

        // 토큰이 없거나 유효하지 않으면 401 반환
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            System.out.println("토큰이 잘못되었거나 누락되었음");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        // 토큰이 유효하면 요청을 계속 진행합니다.
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // preHandle에서 true를 반환한 경우 요청이 처리된 후 호출
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 뷰 렌더링이 완료된 후에 호출
    }
}
