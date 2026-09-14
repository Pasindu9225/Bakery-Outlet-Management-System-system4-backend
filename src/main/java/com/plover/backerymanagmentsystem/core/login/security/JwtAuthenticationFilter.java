package com.plover.backerymanagmentsystem.core.login.security;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.core.login.service.impl.TokenServiceImpl;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenServiceImpl tokenService;
    private final AuthRepository authRepository;

    public JwtAuthenticationFilter(TokenServiceImpl tokenService, AuthRepository authRepository) {
        this.tokenService = tokenService;
        this.authRepository = authRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            try {
                UUID userId = tokenService.extractUserId(jwt);
                AuthModel user = authRepository.findById(IdUtil.uuidToBytes(userId)).orElse(null);
                if (user != null) {
                    String roleId = user.getRoleId();
                    if (roleId != null) {
                        roleId = roleId.trim();
                    }
                    
                    log.info("Authenticating user: {}, Role ID: {}", user.getUsername(), roleId);

                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication = 
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user, null, 
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + (roleId != null ? roleId.toUpperCase() : "UNDEFINED"))));
                    
                    authentication.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("User not found in database for ID: {}", userId);
                }
            } catch (Exception ex) {
                log.error("Authentication failed: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
