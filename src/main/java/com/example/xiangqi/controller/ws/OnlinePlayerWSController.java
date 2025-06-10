package com.example.xiangqi.controller.ws;

import com.example.xiangqi.service.my_sql.OnlinePlayerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Controller
public class OnlinePlayerWSController {
    OnlinePlayerService onlinePlayerService;

    @MessageMapping("/status")
    public void updateStatus(String payload, StompHeaderAccessor headerAccessor) {
        // Update status
        onlinePlayerService.updateStatus(payload, headerAccessor);
    }
}