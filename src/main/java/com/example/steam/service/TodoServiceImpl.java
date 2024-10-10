package com.example.steam.service;

import com.example.steam.entity.Todo;

import com.example.steam.repository.TodoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;

    public TodoServiceImpl(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }


    @Override
    public List<Todo> getTodosByUserId(String userId) {
        return todoRepository.findByUserId(userId);
    }


    // 특정 id로 할 일을 조회하는 메서드
    @Override
    public Todo getTodoById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found with id: " + id));
    }

    // 새로운 할 일을 생성하는 메서드
    @Override
    public Todo createTodo(Todo todo) {
        return todoRepository.save(todo);
    }

    // 기존의 할 일을 수정하는 메서드
    @Override
    public Todo updateTodo(Long id, Todo updatedTodo) {
        Todo currentTodo = getTodoById(id);

        // 제목, 설명, 완료 여부 업데이트
        currentTodo.setTitle(updatedTodo.getTitle());
        currentTodo.setDescription(updatedTodo.getDescription());
        currentTodo.setCompleted(updatedTodo.isCompleted());

        return todoRepository.save(currentTodo);
    }

    // 특정 id의 할 일을 삭제하는 메서드
    @Override
    public void deleteTodoById(Long id) {
        Todo todo = getTodoById(id);
        todoRepository.deleteById(id);
    }


}