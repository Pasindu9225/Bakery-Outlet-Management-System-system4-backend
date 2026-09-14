package com.plover.backerymanagmentsystem.admin.service;

import com.plover.backerymanagmentsystem.admin.dto.AdminUserDto;
import com.plover.backerymanagmentsystem.admin.dto.CreateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateUserRequestDto;

import java.util.List;

public interface AdminUserService {
	List<AdminUserDto> getAllUsers();
	AdminUserDto createUser(CreateUserRequestDto request);
	AdminUserDto updateUser(String id, UpdateUserRequestDto request);
	void deleteUser(String id);
	AdminUserDto updateVerificationCode(String id, String verificationCode);
}


