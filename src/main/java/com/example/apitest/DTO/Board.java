package com.example.apitest.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    private Long id;
    private String title;
    private String content;
    private String writer; // 작성자 필드 추가
    private LocalDateTime date; // 작성일 필드 추가


}
