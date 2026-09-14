package com.plover.backerymanagmentsystem.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVerificationCodeRequestDto {
    @NotBlank(message = "Verification code cannot be empty")
    private String verificationCode;
}
