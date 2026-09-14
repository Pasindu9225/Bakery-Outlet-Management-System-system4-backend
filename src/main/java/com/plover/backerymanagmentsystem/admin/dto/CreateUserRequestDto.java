package com.plover.backerymanagmentsystem.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDto {
    
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;
    
    private String password;
    
    private String phone;
    
    @NotBlank(message = "Role ID is required")
    private String roleId;
    
    @NotNull(message = "Is Active is required")
    private Boolean isActive;

    private String waiterId;

    private Long outletId;
    private Long productionCenterId;
    private Long mpcId;
}

