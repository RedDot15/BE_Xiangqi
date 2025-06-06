package com.example.xiangqi.controller.rest;

import com.example.xiangqi.dto.request.MoveRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.MatchService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/matches")
public class MatchController {
    MatchService matchService;

    @GetMapping("/{matchId}")
    public ResponseEntity<ResponseObject> getMatch(@PathVariable Long matchId) {
        // Fetch board state
        return buildResponse(HttpStatus.OK, "Board state fetch successfully.", matchService.getMatchStateById(matchId));
    }
}
