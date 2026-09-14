package com.plover.backerymanagmentsystem.core.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.core.login.dto.MeResponseDto;
import com.plover.backerymanagmentsystem.core.login.dto.RefreshTokenRequest;
import com.plover.backerymanagmentsystem.core.login.dto.emailPasswordAuthDto;
import com.plover.backerymanagmentsystem.core.login.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bmsauth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/emailPasswordAuth")
    public ResponseEntity<?> oauthLogin(@RequestBody @Valid emailPasswordAuthDto dto) {
        return authService.loginWithEmailAuth(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String accessToken,
            @RequestBody @Valid RefreshTokenRequest dto) {
        return authService.logout(accessToken, dto);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

}
