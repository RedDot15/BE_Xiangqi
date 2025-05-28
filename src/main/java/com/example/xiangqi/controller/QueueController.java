package com.example.xiangqi.controller;

import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.QueueService;
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
@RequestMapping("/api/queue")
public class QueueController {
	QueueService queueService;

	@PostMapping("/join")
	public ResponseEntity<ResponseObject> joinQueue() {
		// Queue
		queueService.queue();
		// Fetch & Return all users
		return buildResponse(HttpStatus.OK, "Queueing completed.", null);
	}

	@DeleteMapping("/cancel")
	public ResponseEntity<ResponseObject> unQueue() {
		// Unqueue
		queueService.unqueue();
		// Fetch & Return all users
		return buildResponse(HttpStatus.OK, "Unqueue success.", null);
	}

}
