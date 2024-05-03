package com.example.steam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Java Object를 JSON 문자열로 변환
    public static String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    // Java Object를 JSON 파일로 저장
    public static void writeJsonToFile(Object obj, String filePath) {
        try {
            objectMapper.writeValue(new File(filePath), obj);
        } catch (IOException e) {
            throw new RuntimeException("파일 쓰기 실패", e);
        }
    }

    // Map을 JSON 문자열로 변환
    public static String convertMapToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Map JSON 변환 실패", e);
        }
    }

    // List를 JSON 문자열로 변환
    public static String convertListToJson(List<?> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("List JSON 변환 실패", e);
        }
    }

    // 배열을 JSON 문자열로 변환
    public static String convertArrayToJson(Object[] array) {
        try {
            return objectMapper.writeValueAsString(array);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("배열 JSON 변환 실패", e);
        }
    }
}

