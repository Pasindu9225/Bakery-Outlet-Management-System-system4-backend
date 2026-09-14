package com.plover.backerymanagmentsystem.core.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class emailPasswordAuthDto {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Hashed Password is required")
    private String hashedPassword;

    private String role;
}
