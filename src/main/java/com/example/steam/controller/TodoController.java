package com.example.steam.controller;

import com.example.steam.entity.Todo;
import com.example.steam.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ResponseEntity<List<Todo>> getAllTodos() {
        String username = getCurrentUsername(); // 로그인한 사용자 ID 가져오기
        return ResponseEntity.ok(todoService.getTodosByUserId(username));
    }

    // 로그인한 유저명을 가져오는 메서드
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    // 특정 할 일 조회
    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(todoService.getTodoById(id));
    }

    // 새로운 할 일 추가
    @PostMapping
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        String username = getCurrentUsername();
        todo.setUserId(username);
        return ResponseEntity.ok(todoService.createTodo(todo));
    }

    // 할 일 수정
    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable("id") Long id, @RequestBody Todo todo) {
        System.out.println("Todo isCompleted from client: " + todo.isCompleted());
        Todo updatedTodo = todoService.updateTodo(id, todo);
        return ResponseEntity.ok(updatedTodo);  // 업데이트된 Todo 반환
    }

    // 할 일 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodoById(@PathVariable("id") Long id) {
        todoService.deleteTodoById(id);
        return ResponseEntity.noContent().build();
    }
}
