package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
	private String id; // UUID string
	private String firstName;
	private String lastName;
	private String username;
	private String email;
	private String phone;
	private String roleId;
	private Boolean isActive;
	private String verificationCode;
	private String waiterId;
	private Long outletId;
	private Long productionCenterId;
	private Long mpcId;
}


