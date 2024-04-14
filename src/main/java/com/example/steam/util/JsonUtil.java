package com.example.steam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String listToJson(List<String> list) throws JsonProcessingException {
        return objectMapper.writeValueAsString(list);
    }

    public static List<String> jsonToList(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<List<String>>(){});
    }


    // 이미지 URL 리스트를 JSON 문자열로 변환하는 메서드 - 이미 정의된 listToJson 활용
    public static String convertListToJson(List<String> imageUrls) {
        try {
            // 이미 JSON 문자열이면 변환 없이 그대로 반환
            new ObjectMapper().readTree(imageUrls.get(0));
            return imageUrls.get(0);
        } catch (IOException e) {
            // JSON이 아니라면, 리스트를 JSON으로 변환합니다.
            try {
                return listToJson(imageUrls);
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException("JSON 변환 중 오류 발생", jsonProcessingException);
            }
        }
    }


}


