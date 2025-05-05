package com.example.xiangqi.controller;

import com.example.xiangqi.entity.PlayerEntity;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.repository.PlayerRepository;
import com.example.xiangqi.service.WebSocketService;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("/api/ws")
public class WebSocketController {

    WebSocketService webSocketService;

    @MessageMapping("/status")
    public ResponseEntity<ResponseObject> updateStatus(String payload, StompHeaderAccessor headerAccessor) {
        // Update status
        webSocketService.updateStatus(payload, headerAccessor);
        // Return
        return buildResponse(HttpStatus.OK, "Update status success.", null);
    }

    @GetMapping("/player/{username}")
    public ResponseEntity<ResponseObject> findPlayer(@PathVariable String username) {
        // Find & Return player
        return buildResponse(HttpStatus.OK, "Find player success.", webSocketService.findPlayer(username));
    }

    @PostMapping("/player/{username}/invite")
    public ResponseEntity<ResponseObject> invite(@PathVariable String username) {
        // Send invitation
        webSocketService.invite(username);
        // Find & Return player
        return buildResponse(HttpStatus.OK, "Invite player success.", null);
    }

    @DeleteMapping({"/player/un-invite","/player/{username}/un-invite"})
    public ResponseEntity<ResponseObject> unInvite(@PathVariable(required = false) String username) {
        // Unsend invitation
        webSocketService.unInvite(username);
        // Find & Return player
        return buildResponse(HttpStatus.OK, "Un-invite player success.", null);
    }

    @PostMapping("/player/{username}/invitation-accept")
    public ResponseEntity<ResponseObject> acceptInvite(@PathVariable String username) {
        // Accept invitation
        webSocketService.acceptInvitation(username);
        // Find & Return player
        return buildResponse(HttpStatus.OK, "Accept invitation success.", null);
    }

    @DeleteMapping("/player/{username}/invitation-reject")
    public ResponseEntity<ResponseObject> rejectInvite(@PathVariable String username) {
        // Reject invitation
        webSocketService.rejectInvitation(username);
        // Find & Return player
        return buildResponse(HttpStatus.OK, "Reject invitation success.", null);
    }



}