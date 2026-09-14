package com.plover.backerymanagmentsystem.core.login.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponseDto {
    private UUID userId;
    private String username;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Long outletId;
    private String outletName;
    private Long productionCenterId;
    private String productionCenterName;
    private String productionCenterType; // "BAKERY" or "KITCHEN" or null
    private Long mpcId;
    private String mpcName;
}
