package com.example.steam.repository;

import com.example.steam.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserId(String userId);  // user_id에 따라 Todo를 필터링
}