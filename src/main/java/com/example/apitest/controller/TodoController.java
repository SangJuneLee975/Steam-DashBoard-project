package com.example.apitest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TodoController {


    @GetMapping("/Todo")
    public String showData() {
        // 외부 API 호출
        String apiUrl = "https://jsonplaceholder.typicode.com/todos/2";

        RestTemplate restTemplate = new RestTemplate();  //RestTemplate 객체 생성

        String result = restTemplate.getForObject(apiUrl, String.class);
         // 외부 API에 GET 요청을 보내고 결과를 문자열로 받아옴


        return result;
    }
}

