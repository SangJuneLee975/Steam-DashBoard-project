package com.example.steam.service;

import com.example.steam.entity.Todo;

import java.util.List;

public interface TodoService {



    Todo getTodoById(Long id);   // 특정 id를 통해 Todo 조회

    Todo createTodo(Todo todo); // Todo 생성

    Todo updateTodo(Long id, Todo updatedTodo); // Todo 수정

    void deleteTodoById(Long id); //Todo 삭제

    List<Todo> getTodosByUserId(String userId);  // user_id에 따라 Todo 목록 조회
}
