package com.example.xiangqi.controller.ws;

import com.example.xiangqi.dto.request.QueueJoinRequest;
import com.example.xiangqi.dto.request.QueueLeaveRequest;
import com.example.xiangqi.service.my_sql.QueueService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Controller
public class QueueWSController {
	QueueService queueService;

	@MessageMapping("/queue.join")
	public void joinQueue(@Valid QueueJoinRequest request) {
		// Queue
		queueService.joinQueue(request);
	}

	@MessageMapping("/queue.leave")
	public void leaveQueue(QueueLeaveRequest request) {
		// Unqueue
		queueService.leaveQueue(request);
	}
}
