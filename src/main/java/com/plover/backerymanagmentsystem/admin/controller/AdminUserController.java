package com.plover.backerymanagmentsystem.admin.controller;

import com.plover.backerymanagmentsystem.admin.dto.AdminUserDto;
import com.plover.backerymanagmentsystem.admin.dto.CreateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ADMIN/v1")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

	private final AdminUserService adminUserService;

	@GetMapping("/users")
	public ResponseEntity<List<AdminUserDto>> getAllUsers() {
		log.info("Admin: fetching all users");
		return ResponseEntity.ok(adminUserService.getAllUsers());
	}

	@PostMapping("/users")
	public ResponseEntity<AdminUserDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
		log.info("Admin: creating new user with username: {}", request.getUsername());
		AdminUserDto createdUser = adminUserService.createUser(request);
		return ResponseEntity.ok(createdUser);
	}

	@PutMapping("/users/{id}")
	public ResponseEntity<AdminUserDto> updateUser(
			@PathVariable String id,
			@Valid @RequestBody UpdateUserRequestDto request) {
		log.info("Admin: updating user with id: {}", id);
		AdminUserDto updatedUser = adminUserService.updateUser(id, request);
		return ResponseEntity.ok(updatedUser);
	}

	@DeleteMapping("/users/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable String id) {
		log.info("Admin: deleting user with id: {}", id);
		adminUserService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}
}


