package com.example.apitest.repository;

import com.example.apitest.DTO.User;

import java.util.List;

public interface UserRepository {

    public List<User> findAll();

    public void insert(User user);

    // 사용자 아이디로 사용자 조회
    public User findByUserId(String userId);



}