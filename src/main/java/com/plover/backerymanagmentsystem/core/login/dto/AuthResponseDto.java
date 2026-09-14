package com.plover.backerymanagmentsystem.core.login.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDto {

    private UUID userId;
    private String role;
    private String phone;
    private String username;
    private String email;
    private Long outletId;
    private Long productionCenterId;
    private Long mpcId;
    private String firstName;
    private String lastName;
    private String accessToken;
    private String refreshToken;
}
