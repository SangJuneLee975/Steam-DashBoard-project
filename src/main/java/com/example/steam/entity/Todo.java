package com.example.steam.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 고유 ID (Primary Key, 자동 증가)

    @Column(nullable = false)
    private String title;  // 할 일의 제목

    @Column(nullable = true)
    private String description;  // 할 일의 내용

    @JsonProperty("isCompleted")  // 직렬화
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;  // 할 일 완료 여부

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // 할 일이 생성된 날짜

    @Column(name = "user_id", nullable = false)
    private String userId;
}
