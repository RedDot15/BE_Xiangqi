package com.example.xiangqi.controller;

import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.PlayerService;
import com.example.xiangqi.validation.group.Create;
import com.example.xiangqi.validation.group.Update;
import jakarta.validation.groups.Default;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("/api/player")
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
	public ResponseEntity<ResponseObject> register(
			@Validated({Create.class, Default.class}) @RequestBody PlayerRequest playerRequest) {
		// Create & Return user
		return buildResponse(HttpStatus.OK, "Created new player successfully.", playerService.register(playerRequest));
	}

	@PutMapping("/change-password")
	public ResponseEntity<ResponseObject> changePassword(
			@Validated({Update.class, Default.class}) @RequestBody PlayerRequest playerRequest) {
		// Update & Return user
		return buildResponse(HttpStatus.OK, "Changed password successfully.", playerService.changePassword(playerRequest));
	}

	@DeleteMapping(value = "/{userId}/delete")
	public ResponseEntity<ResponseObject> delete(@PathVariable Long userId) {
		// Delete & Return id
		return buildResponse(HttpStatus.OK, "Deleted user successfully.", playerService.delete(userId));
	}

}
