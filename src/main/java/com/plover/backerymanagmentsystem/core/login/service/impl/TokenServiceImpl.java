package com.plover.backerymanagmentsystem.core.login.service.impl;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.core.login.dto.AuthResponseDto;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.model.RefreshToken;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.core.login.repository.RefreshTokenRepository;
import com.plover.backerymanagmentsystem.core.login.service.TokenService;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthRepository authRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final long accessTokenValidityMinutes = 480;
    private final long refreshTokenValidityDays = 7;

    @Override
    public AuthResponseDto generateTokens(AuthModel user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = generateAccessToken(user);
        RefreshToken refreshToken = generateAndStoreRefreshToken(user);

        return new AuthResponseDto(
                IdUtil.bytesToUuid(user.getId()),
                user.getRoleId(),
                user.getPhone(),
                user.getUsername(),
                user.getEmail(),
                user.getOutletId(),
                user.getProductionCenterId(),
                user.getMpcId(),
                user.getFirstName(),
                user.getLastName(),
                accessToken,
                refreshToken.getToken()
        );
    }

    private String generateAccessToken(AuthModel user) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

        return Jwts.builder()
                .setSubject(IdUtil.bytesToUuidString(user.getId()))
                .claim("role", user.getRoleId())
                .claim("username", user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenValidityMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private RefreshToken generateAndStoreRefreshToken(AuthModel user) {
        String rawToken = UUID.randomUUID() + "." + generateRandomBase64(32);
        Instant expiry = Instant.now().plus(refreshTokenValidityDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(rawToken)
                .expiryDate(expiry)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public AuthResponseDto refreshTokens(String token) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByToken(token);

        if (existingToken.isEmpty() || existingToken.get().isRevoked()
                || existingToken.get().getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        RefreshToken old = existingToken.get();
        old.setRevoked(true);
        refreshTokenRepository.save(old);

        byte[] userId = old.getUserId();
        AuthModel user = authRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        return generateTokens(user);
    }

    private String generateRandomBase64(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID extractUserId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        String userId = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return UUID.fromString(userId);
    }
}
