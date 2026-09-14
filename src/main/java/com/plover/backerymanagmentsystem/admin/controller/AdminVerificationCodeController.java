package com.plover.backerymanagmentsystem.admin.controller;

import com.plover.backerymanagmentsystem.admin.dto.AdminUserDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateVerificationCodeRequestDto;
import com.plover.backerymanagmentsystem.admin.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ADMIN/v1/verification-codes")
@RequiredArgsConstructor
@Slf4j
public class AdminVerificationCodeController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUserVerificationCodes() {
        log.info("Admin: fetching all users for verification code management");
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateVerificationCode(
            @PathVariable String id,
            @Valid @RequestBody UpdateVerificationCodeRequestDto request) {
        log.info("Admin: updating verification code for user id: {}", id);
        AdminUserDto updatedUser = adminUserService.updateVerificationCode(id, request.getVerificationCode());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AdminUserDto> deleteVerificationCode(@PathVariable String id) {
        log.info("Admin: removing verification code for user id: {}", id);
        AdminUserDto updatedUser = adminUserService.updateVerificationCode(id, null);
        return ResponseEntity.ok(updatedUser);
    }
}
