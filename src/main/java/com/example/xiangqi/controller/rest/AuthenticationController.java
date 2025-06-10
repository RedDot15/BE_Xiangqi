package com.example.xiangqi.controller.rest;

import com.example.xiangqi.dto.request.AuthenticationRequest;
import com.example.xiangqi.dto.request.OutboundAuthenticationRequest;
import com.example.xiangqi.dto.request.RefreshRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.my_sql.AuthenticationService;
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

	@PostMapping("/outbound/{provider}/authenticate")
	public ResponseEntity<ResponseObject> outboundAuthenticate(@PathVariable String provider, @RequestBody OutboundAuthenticationRequest request){
		return buildResponse(HttpStatus.OK, "Outbound authenticate successfully.", authenticationService.outboundAuthenticate(request));
	}

	@PostMapping(value = "/tokens")
	public ResponseEntity<ResponseObject> authenticate(@Valid @RequestBody AuthenticationRequest request) {
		return buildResponse(HttpStatus.OK, "Authenticate successfully.", authenticationService.authenticate(request));
	}

	@PostMapping("/tokens/refresh")
	public ResponseEntity<ResponseObject> refreshToken(@Valid @RequestBody RefreshRequest request) {
		return buildResponse(HttpStatus.OK, "Refresh token successfully.", authenticationService.refresh(request));
	}

	@GetMapping("/tokens/introspect")
	public ResponseEntity<ResponseObject> introspect() {
		return buildResponse(HttpStatus.OK, "Token valid.", null);
	}

	@DeleteMapping("/tokens")
	public ResponseEntity<ResponseObject> logout() {
		authenticationService.logout();
		return buildResponse(HttpStatus.OK, "Log out successfully.", null);
	}
}
