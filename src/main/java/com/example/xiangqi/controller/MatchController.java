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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("/api/match")
public class MatchController {
    MatchService matchService;

    @GetMapping("/")
    public ResponseEntity<ResponseObject> getAllFinished(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId) {
        // Fetch match list
        return buildResponse(HttpStatus.OK, "Matches fetch successfully.", matchService.getAllFinished(page, size, userId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ResponseObject> getMatch(@PathVariable Long matchId) {
        // Fetch board state
        return buildResponse(HttpStatus.OK, "Board state fetch successfully.", matchService.getMatchStateById(matchId));
    }

    @PostMapping("/ai")
    public ResponseEntity<ResponseObject> createMatchAgainstAI(@RequestBody CreateAIMatchRequest request) {

        return buildResponse(HttpStatus.OK, "MATCH_CREATED_WITH_AI", matchService.createMatchWithAI(request));
    }

    @PostMapping("/{matchId}/ready")
    public ResponseEntity<ResponseObject> readyMatch(@PathVariable Long matchId) {
        // Ready match
        matchService.ready(matchId);
        // Response
        return buildResponse(HttpStatus.OK, "Ready match successfully.", null);
    }

    @PostMapping("/{matchId}/move")
    public ResponseEntity<ResponseObject> move(@PathVariable Long matchId, @RequestBody @Valid MoveRequest moveRequest) {
        // Handle move request
        matchService.move(matchId, moveRequest);
        // Response
        return buildResponse(HttpStatus.OK, "Move request successfully.", null);
    }

    @PutMapping("/{matchId}/resign")
    public ResponseEntity<ResponseObject> resignGame(@PathVariable Long matchId) {
        // Handle resign request
        matchService.resign(matchId);
        // Handle move request
        return buildResponse(HttpStatus.OK, "Move request successfully.", null);
    }
}
