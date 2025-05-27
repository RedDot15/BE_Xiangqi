package com.example.xiangqi.service;

import com.example.xiangqi.dto.model.Position;
import com.example.xiangqi.dto.request.MoveRequest;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class AIService {
    public MoveRequest callAIMove(String[][] boardState, String aiMode) {
        try {
            String url = switch (aiMode) {
                case "AI_EASY" -> "http://127.0.0.1:8000/next-move";
                case "AI_HARD" -> "http://127.0.0.1:8000/next-move-pikafish";
                default -> throw new IllegalArgumentException("Unknown AI mode: " + aiMode);
            };
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gửi mảng boardState trực tiếp dưới dạng JSON
            Map<String, Object> boardWrapper = new HashMap<>();
            boardWrapper.put("board", boardState);  // Truyền thẳng mảng 2 chiều boardState

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(boardWrapper, headers);

            // Gọi API Python
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Integer> moveMap = response.getBody();

                Position from = new Position(moveMap.get("from_row"), moveMap.get("from_col"));
                Position to = new Position(moveMap.get("to_row"), moveMap.get("to_col"));

                return new MoveRequest(from, to);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // hoặc throw nếu cần
    }
}
