package com.plover.backerymanagmentsystem.core.login.service;

import org.springframework.http.ResponseEntity;

import com.plover.backerymanagmentsystem.core.login.dto.MeResponseDto;
import com.plover.backerymanagmentsystem.core.login.dto.RefreshTokenRequest;
import com.plover.backerymanagmentsystem.core.login.dto.emailPasswordAuthDto;

public interface AuthService {

    ResponseEntity<?> loginWithEmailAuth(emailPasswordAuthDto dto);

    ResponseEntity<?> refreshAccessToken(RefreshTokenRequest dto);

    ResponseEntity<?> logout(String accessToken, RefreshTokenRequest dto);

    MeResponseDto getCurrentUser();

}
