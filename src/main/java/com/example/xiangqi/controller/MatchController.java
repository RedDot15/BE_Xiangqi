package com.example.xiangqi.controller;

import com.example.xiangqi.dto.request.CreateAIMatchRequest;
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

    @PatchMapping("/{matchId}")
    public ResponseEntity<ResponseObject> move(@PathVariable Long matchId, @RequestBody @Valid MoveRequest moveRequest) {
        // Handle move request
        matchService.move(matchId, moveRequest);
        // Response
        return buildResponse(HttpStatus.OK, "Move request successfully.", null);
    }

    @PatchMapping("/{matchId}/resign")
    public ResponseEntity<ResponseObject> resignGame(@PathVariable Long matchId) {
        // Handle resign request
        matchService.resign(matchId);
        // Handle move request
        return buildResponse(HttpStatus.OK, "Move request successfully.", null);
    }
}
