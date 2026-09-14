package com.plover.backerymanagmentsystem.core.login.service.impl;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.core.login.dto.AuthResponseDto;
import com.plover.backerymanagmentsystem.core.login.dto.MeResponseDto;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.core.login.dto.RefreshTokenRequest;
import com.plover.backerymanagmentsystem.core.login.dto.emailPasswordAuthDto;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.model.RefreshToken;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.core.login.repository.RefreshTokenRepository;
import com.plover.backerymanagmentsystem.core.login.service.AuthService;
import com.plover.backerymanagmentsystem.core.login.service.TokenService;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final OutletRepository outletRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final OutletProductionCenterRepository outletProductionCenterRepository;

    @Override
    public ResponseEntity<?> loginWithEmailAuth(emailPasswordAuthDto dto) {
        String username = dto.getEmail();

        // Hardcode restriction: Superadmin can ONLY log into the Training System, NEVER the Real System.
        if ("superadmin".equalsIgnoreCase(username) || "superadmin@bakery.com".equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access Denied: Super Admin is strictly restricted to the Training System.");
        }

        String providedPassword = dto.getHashedPassword();
        String role = dto.getRole() != null ? dto.getRole() : "undefined";

        Optional<AuthModel> userOpt = authRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        AuthModel user = userOpt.get();
        String storedHash = user.getPasswordHash();
        boolean passwordMatch = BCrypt.checkpw(providedPassword, storedHash);
        if (!passwordMatch) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        // Block inactive users from logging in
        if (!user.isActive()) {
            log.warn("Login attempt by inactive user: {}", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is inactive. Please contact your administrator.");
        }
        AuthResponseDto authResponse = tokenService.generateTokens(user);
        return ResponseEntity.ok(authResponse);
    }

    @Override
    public ResponseEntity<?> refreshAccessToken(RefreshTokenRequest dto) {
        try {
            AuthResponseDto newTokens = tokenService.refreshTokens(dto.getRefreshToken());
            return ResponseEntity.ok(newTokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }
    }

    @Override
    public ResponseEntity<?> logout(String accessToken, RefreshTokenRequest dto) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(dto.getRefreshToken());

        if (refreshTokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh token not found");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok("Logged out successfully");
    }

    @Override
    public MeResponseDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AuthModel u = (AuthModel) auth.getPrincipal();

        String outletName = null;
        if (u.getOutletId() != null) {
            outletName = outletRepository.findById(u.getOutletId())
                    .map(Outlet::getName).orElse(null);
        }

        Long pcId = u.getProductionCenterId();
        String pcName = null;
        String pcType = null;
        if (pcId != null) {
            ProductionCenter pc = productionCenterRepository.findById(pcId).orElse(null);
            if (pc != null) {
                pcName = pc.getCenterName();
                pcType = pc.getType() != null ? pc.getType().name() : null;
            }
        }

        Long mpcId = u.getMpcId();
        String mpcName = null;
        if (mpcId != null) {
            OutletProductionCenter mpc = outletProductionCenterRepository.findById(mpcId).orElse(null);
            if (mpc != null) {
                mpcName = mpc.getName();
            }
        }

        return MeResponseDto.builder()
                .userId(IdUtil.bytesToUuid(u.getId()))
                .username(u.getUsername())
                .role(u.getRoleId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .outletId(u.getOutletId())
                .outletName(outletName)
                .productionCenterId(pcId)
                .productionCenterName(pcName)
                .productionCenterType(pcType)
                .mpcId(mpcId)
                .mpcName(mpcName)
                .build();
    }

}
