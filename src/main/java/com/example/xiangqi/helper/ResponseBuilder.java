package com.example.xiangqi.helper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseBuilder {
	public static ResponseEntity<ResponseObject> buildResponse(HttpStatus status, String message, Object data) {
		return ResponseEntity.status(status)
				.body(new ResponseObject(status.is2xxSuccessful() ? "ok" : "failed", message, data));
	}
}
