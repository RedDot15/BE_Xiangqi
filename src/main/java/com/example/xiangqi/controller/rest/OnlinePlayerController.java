package com.example.xiangqi.controller.rest;

import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.my_sql.OnlinePlayerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("/api/online-players")
public class OnlinePlayerController {
    OnlinePlayerService onlinePlayerService;

    @GetMapping("/{username}")
    public ResponseEntity<ResponseObject> getPlayer(@PathVariable String username) {
        // Find & Return player
        return buildResponse(HttpStatus.OK, "Find player success.", onlinePlayerService.getPlayer(username));
    }
}