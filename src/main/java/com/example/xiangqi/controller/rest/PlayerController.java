package com.example.xiangqi.controller.rest;

import com.example.xiangqi.dto.request.ChangePasswordRequest;
import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.dto.request.SocialPlayerRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.my_sql.PlayerService;
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
@RequestMapping("/api/players")
public class PlayerController {
	PlayerService playerService;

	@GetMapping("")
	public ResponseEntity<ResponseObject> getAll(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String role) {
		// Fetch & Return all users
		return buildResponse(HttpStatus.OK, "Get all player successfully.", playerService.getAll(page, size, role));
	}

	@GetMapping("/me")
	public ResponseEntity<ResponseObject> getMyInfo() {
		// Fetch & Return all users
		return buildResponse(HttpStatus.OK, "My information fetch successfully.", playerService.getMyInfo());
	}

	@PostMapping("")
	public ResponseEntity<ResponseObject> register(@Valid @RequestBody PlayerRequest playerRequest) {
		// Create & Return user
		return buildResponse(HttpStatus.OK, "Created new player successfully.", playerService.register(playerRequest));
	}

	@PostMapping("/social")
	public ResponseEntity<ResponseObject> socialRegister(@Valid @RequestBody SocialPlayerRequest request) {
		// Create & Return user
		return buildResponse(HttpStatus.OK, "Created new player successfully.", playerService.socialRegister(request));
	}

	@PutMapping("/me")
	public ResponseEntity<ResponseObject> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		// Update & Return user
		return buildResponse(HttpStatus.OK, "Changed password successfully.", playerService.changePassword(request));
	}

	@DeleteMapping(value = "/{playerId}")
	public ResponseEntity<ResponseObject> delete(@PathVariable Long playerId) {
		// Delete & Return id
		return buildResponse(HttpStatus.OK, "Deleted user successfully.", playerService.delete(playerId));
	}
}
