package com.example.apitest.controller;

import com.example.apitest.DTO.User;
import com.example.apitest.service.UserService;
import com.example.apitest.config.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Autowired
    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/add")
    public ResponseEntity<String> addUser(@RequestBody User user) {
        userService.insertUser(user);
        return ResponseEntity.ok("사용자가 성공적으로 추가");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        String userId = loginRequest.getUserId();
        String password = loginRequest.getPassword();

        boolean isAuthenticated = userService.authenticateUser(userId, password);

        if (isAuthenticated) {
            // 사용자 정보 가져오기
            User user = userService.findByUserId(userId);
            // 사용자 정보를 포함하여 토큰 생성
            String token = jwtUtils.generateToken(userId, user.getName(), user.getNickname()); // 사용자 이름과 별명 추가

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("잘못된 자격 증명");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // 클라이언트에서 JWT를 삭제하도록 하면 됩니다.
        return ResponseEntity.ok("로그아웃 성공");
    }


}
