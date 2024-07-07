package com.example.apitest.repository;

import com.example.apitest.DTO.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


@Repository
public class JdbcTemplateRepository implements UserRepository {

    // JdbcTemplate 인스턴스를 주입받는 생성자
    private final JdbcTemplate jdbcTemplate;

    public JdbcTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public User findByUserId(String userId) {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM user WHERE userId = ?",
                new Object[]{userId},
                (resultSet, rowNum) -> {
                    User user = new User();
                    user.setUserId(resultSet.getString("userId"));
                    user.setPassword(resultSet.getString("password"));
                    user.setName(resultSet.getString("name"));
                    user.setNickname(resultSet.getString("nickname"));
                    return user;
                });

        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM user";
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
    public void insert(User user) {
        String sql = "INSERT INTO user (userId, password, name, nickname) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getUserId(), user.getPassword(), user.getName(), user.getNickname());
    }
}