package com.example.xiangqi.controller;

import com.example.xiangqi.dto.request.ChangePasswordRequest;
import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.PlayerService;
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

	@GetMapping("/my-info/get")
	public ResponseEntity<ResponseObject> showMyInfo() {
		// Fetch & Return all users
		return buildResponse(HttpStatus.OK, "My information fetch successfully.", playerService.getMyInfo());
	}

	@GetMapping("/")
	public ResponseEntity<ResponseObject> getAll(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String role) {
		// Fetch & Return all users
		return buildResponse(HttpStatus.OK, "Get all player successfully.", playerService.getAll(page, size, role));
	}

	@PostMapping("/register")
	public ResponseEntity<ResponseObject> register(@Valid @RequestBody PlayerRequest playerRequest) {
		// Create & Return user
		return buildResponse(HttpStatus.OK, "Created new player successfully.", playerService.register(playerRequest));
	}

	@PutMapping("/change-password")
	public ResponseEntity<ResponseObject> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		// Update & Return user
		return buildResponse(HttpStatus.OK, "Changed password successfully.", playerService.changePassword(request));
	}

	@DeleteMapping(value = "/{userId}/delete")
	public ResponseEntity<ResponseObject> delete(@PathVariable Long userId) {
		// Delete & Return id
		return buildResponse(HttpStatus.OK, "Deleted user successfully.", playerService.delete(userId));
	}

}
