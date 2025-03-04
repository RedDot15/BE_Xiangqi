package com.example.xiangqi.config.security;

import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.ResponseObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	@Override
	public void commence(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		// Get error code
		ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
		// Set response
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		// new ResponseObject
		ResponseObject responseObject = new ResponseObject("failed", errorCode.getMessage(), null);
		// Mapper
		ObjectMapper objectMapper = new ObjectMapper();
		// Write response
		response.getWriter().write(objectMapper.writeValueAsString(responseObject));
		response.flushBuffer();
	}
}
