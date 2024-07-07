// UserServiceImpl.java
package com.example.apitest.service;

import com.example.apitest.DTO.User;
import com.example.apitest.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.PreparedStatement;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public UserServiceImpl(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT userId, password, name, nickname FROM user"; // date 필드를 제외한 필드를 조회합니다.
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            User user = new User();
            user.setUserId(resultSet.getString("userId"));
            user.setPassword(resultSet.getString("password"));
            user.setName(resultSet.getString("name"));
            user.setNickname(resultSet.getString("nickname"));
            return user;
        });
    }

    @Override
    public void insertUser(User user) {
        String sql = "INSERT INTO user (userId, password, name, nickname) VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, user.getUserId());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getName()); // 사용자의 이름을 설정합니다.
            preparedStatement.setString(4, user.getNickname()); // 사용자의 닉네임을 설정합니다.
            return preparedStatement;
        });
    }

    @Override
    public User findByUserId(String userId) {
        System.out.println("Attempting to find user by ID: " + userId); // 사용자 ID 로그 출력
        User user = userRepository.findByUserId(userId);
        if (user != null) {
            System.out.println("User found: " + user.getUserId()); // 사용자 ID 로그 출력
        } else {
            System.out.println("User not found for ID: " + userId); // 사용자 ID 로그 출력
        }
        return user;
    }

    @Override
    public boolean authenticateUser(String userId, String password) {
        // 실제 사용자 정보를 데이터베이스에서 조회하여 인증합니다.
        // UserRepository를 사용하여 사용자 정보를 조회합니다.
        User user = findByUserId(userId);

        // 사용자가 존재하고 비밀번호가 일치하는지 확인합니다.
        return user != null && user.getPassword().equals(password);
    }
}
