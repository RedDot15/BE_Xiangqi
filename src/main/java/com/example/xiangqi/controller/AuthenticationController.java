package com.example.xiangqi.controller;

import com.example.xiangqi.dto.request.AuthenticationRequest;
import com.example.xiangqi.dto.request.RefreshRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.AuthenticationService;
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
@RequestMapping("/api/auth")
public class AuthenticationController {
	AuthenticationService authenticationService;

	@GetMapping("/token/get")
	public ResponseEntity<ResponseObject> authenticate(@Valid @RequestBody AuthenticationRequest request) {
		return buildResponse(HttpStatus.OK, "Authenticate successfully.", authenticationService.authenticate(request));
	}

	@PostMapping("/token/refresh")
	public ResponseEntity<ResponseObject> refreshToken(@Valid @RequestBody RefreshRequest request) {
		return buildResponse(HttpStatus.OK, "Authenticate successfully.", authenticationService.refresh(request));
	}

	@PostMapping("/my-token/invalidate")
	public ResponseEntity<ResponseObject> logout() {
		authenticationService.logout();
		return buildResponse(HttpStatus.OK, "Log out successfully.", null);
	}
}
