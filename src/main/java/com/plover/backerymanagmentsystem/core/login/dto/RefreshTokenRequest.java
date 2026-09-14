package com.plover.backerymanagmentsystem.core.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
