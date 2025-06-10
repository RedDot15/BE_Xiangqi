package com.example.xiangqi.controller.ws;

import com.example.xiangqi.dto.request.MoveRequest;
import com.example.xiangqi.dto.request.ResignRequest;
import com.example.xiangqi.service.my_sql.MatchService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Controller
public class MatchWSController {
    MatchService matchService;

    @MessageMapping("/match.make-move")
    public void updateMatch(@Valid MoveRequest moveRequest) {
        // Handle move
        matchService.move(moveRequest);
    }

    @MessageMapping("/match.resign")
    public void updateMatch(@Valid ResignRequest resignRequest) {
        // Handle resign request
        matchService.resign(resignRequest);
    }
}
