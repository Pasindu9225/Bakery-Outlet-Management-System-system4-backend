package com.plover.backerymanagmentsystem.admin.service.impl;

import com.plover.backerymanagmentsystem.admin.dto.AdminUserDto;
import com.plover.backerymanagmentsystem.admin.dto.CreateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import com.plover.backerymanagmentsystem.admin.repository.BmsAuthRepository;
import com.plover.backerymanagmentsystem.admin.service.AdminUserService;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

	private final BmsAuthRepository bmsAuthRepository;
	private final OutletRepository outletRepository;
	private final ProductionCenterRepository productionCenterRepository;
	private final OutletProductionCenterRepository outletProductionCenterRepository;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Override
	public List<AdminUserDto> getAllUsers() {
		List<BmsAuth> users = bmsAuthRepository.findAll();
		return users.stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Override
	public AdminUserDto createUser(CreateUserRequestDto request) {
		// Validate role-specific requirements
		// Note: "11" or "WAITER" could be the ID. 
		// I'll check if it's NOT a waiter to enforce login details.
		boolean isWaiter = "WAITER".equalsIgnoreCase(request.getRoleId()) || "11".equals(request.getRoleId());
		
		if (!isWaiter) {
			if (request.getUsername() == null || request.getUsername().isBlank()) {
				throw new RuntimeException("Username is required for non-waiter roles");
			}
			if (request.getPassword() == null || request.getPassword().isBlank()) {
				throw new RuntimeException("Password is required for non-waiter roles");
			}
		}

		validateRoleBindings(request.getRoleId(), request.getOutletId(),
				request.getProductionCenterId(), request.getMpcId());

		// Generate UUID and convert to byte array
		UUID uuid = UUID.randomUUID();
		byte[] idBytes = IdUtil.uuidToBytes(uuid);
		
		// Hash the password only if provided
		String passwordHash = null;
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			passwordHash = passwordEncoder.encode(request.getPassword());
		}

		// Handle empty/blank fields for Waiter or other roles to avoid duplicate unique constraints
		String username = request.getUsername();
		if (isWaiter && (username == null || username.isBlank())) {
			username = null;
		} else if (username != null && username.isBlank()) {
			username = null;
		}

		String waiterId = request.getWaiterId();
		if (waiterId != null && waiterId.isBlank()) {
			waiterId = null;
		}
		
		// Create entity
		BmsAuth user = BmsAuth.builder()
				.id(idBytes)
				.username(username)
				.email(request.getEmail())
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.passwordHash(passwordHash)
				.phone(request.getPhone())
				.roleId(request.getRoleId())
				.isActive(request.getIsActive())
				.waiterId(waiterId)
				.outletId(request.getOutletId())
				.productionCenterId(request.getProductionCenterId())
				.mpcId(request.getMpcId())
				.build();
		
		// Save to database
		BmsAuth savedUser = bmsAuthRepository.save(user);
		
		// Convert to DTO and return
		return toDto(savedUser);
	}

	@Override
	public AdminUserDto updateUser(String id, UpdateUserRequestDto request) {
		// Convert UUID string to byte array
		UUID uuid = UUID.fromString(id);
		byte[] idBytes = IdUtil.uuidToBytes(uuid);
		
		// Find existing user
		Optional<BmsAuth> existingUserOpt = bmsAuthRepository.findById(idBytes);
		if (existingUserOpt.isEmpty()) {
			throw new RuntimeException("User not found with id: " + id);
		}
		
		BmsAuth existingUser = existingUserOpt.get();
		
		// Update fields
		existingUser.setFirstName(request.getFirstName());
		existingUser.setLastName(request.getLastName());
		existingUser.setEmail(request.getEmail());
		existingUser.setPhone(request.getPhone());
		existingUser.setRoleId(request.getRoleId());
		existingUser.setIsActive(request.getIsActive());
		
		String waiterId = request.getWaiterId();
		if (waiterId != null && waiterId.isBlank()) {
			waiterId = null;
		}
		existingUser.setWaiterId(waiterId);

		validateRoleBindings(request.getRoleId(), request.getOutletId(),
				request.getProductionCenterId(), request.getMpcId());
		existingUser.setOutletId(request.getOutletId());
		existingUser.setProductionCenterId(request.getProductionCenterId());
		existingUser.setMpcId(request.getMpcId());

		// Update password only if provided
		if (request.getPassword() != null && !request.getPassword().isEmpty()) {
			String passwordHash = passwordEncoder.encode(request.getPassword());
			existingUser.setPasswordHash(passwordHash);
		}
		
		// Save updated user
		BmsAuth updatedUser = bmsAuthRepository.save(existingUser);
		
		// Convert to DTO and return
		return toDto(updatedUser);
	}

	@Override
	public void deleteUser(String id) {
		// Convert UUID string to byte array
		UUID uuid = UUID.fromString(id);
		byte[] idBytes = IdUtil.uuidToBytes(uuid);
		
		// Check if user exists
		Optional<BmsAuth> existingUserOpt = bmsAuthRepository.findById(idBytes);
		if (existingUserOpt.isEmpty()) {
			throw new RuntimeException("User not found with id: " + id);
		}
		
		// Delete user from database
		bmsAuthRepository.deleteById(idBytes);
	}

	@Override
	public AdminUserDto updateVerificationCode(String id, String verificationCode) {
		// Convert UUID string to byte array
		UUID uuid = UUID.fromString(id);
		byte[] idBytes = IdUtil.uuidToBytes(uuid);
		
		// Find existing user
		Optional<BmsAuth> existingUserOpt = bmsAuthRepository.findById(idBytes);
		if (existingUserOpt.isEmpty()) {
			throw new RuntimeException("User not found with id: " + id);
		}
		
		BmsAuth existingUser = existingUserOpt.get();
		
		// Check for uniqueness of verification code if not null/blank
		if (verificationCode != null && !verificationCode.trim().isEmpty()) {
			Optional<BmsAuth> userWithSameCode = bmsAuthRepository.findByVerificationCode(verificationCode.trim());
			if (userWithSameCode.isPresent() && !java.util.Arrays.equals(userWithSameCode.get().getId(), idBytes)) {
				throw new IllegalArgumentException("Verification code '" + verificationCode + "' is already in use by another user.");
			}
		}

		existingUser.setVerificationCode(verificationCode);
		
		// Save updated user
		BmsAuth updatedUser = bmsAuthRepository.save(existingUser);
		
		// Convert to DTO and return
		return toDto(updatedUser);
	}

	private void validateRoleBindings(String roleId, Long outletId, Long productionCenterId, Long mpcId) {
		boolean isCashier = "8".equals(roleId) || "POS Cashier".equalsIgnoreCase(roleId);
		boolean isBakeryWorker = "12".equals(roleId) || "Bakery Worker".equalsIgnoreCase(roleId);
		boolean isKitchenWorker = "13".equals(roleId) || "Kitchen Worker".equalsIgnoreCase(roleId);
		boolean isMpcWorker = "14".equals(roleId) || "MPC Worker".equalsIgnoreCase(roleId);

		if (isCashier) {
			if (outletId == null) {
				throw new RuntimeException("Outlet is required for cashier role");
			}
			if (outletRepository.findById(outletId).isEmpty()) {
				throw new RuntimeException("Outlet not found with id: " + outletId);
			}
			if (productionCenterId != null) {
				throw new RuntimeException("Production center must not be set for cashier role");
			}
			if (mpcId != null) {
				throw new RuntimeException("MPC must not be set for cashier role");
			}
		} else if (isBakeryWorker) {
			if (outletId != null) {
				throw new RuntimeException("Outlet must not be set for Bakery Worker role");
			}
			if (mpcId != null) {
				throw new RuntimeException("MPC must not be set for Bakery Worker role");
			}
			requireMainPC(productionCenterId, ProductionCenterType.BAKERY, "Bakery Worker");
		} else if (isKitchenWorker) {
			if (outletId != null) {
				throw new RuntimeException("Outlet must not be set for Kitchen Worker role");
			}
			if (mpcId != null) {
				throw new RuntimeException("MPC must not be set for Kitchen Worker role");
			}
			requireMainPC(productionCenterId, ProductionCenterType.KITCHEN, "Kitchen Worker");
		} else if (isMpcWorker) {
			if (outletId == null) {
				throw new RuntimeException("Outlet is required for MPC Worker role");
			}
			if (outletRepository.findById(outletId).isEmpty()) {
				throw new RuntimeException("Outlet not found with id: " + outletId);
			}
			if (productionCenterId != null) {
				throw new RuntimeException("Production center must not be set for MPC Worker role");
			}
			requireMpcInOutlet(mpcId, outletId);
		} else if (outletId != null) {
			if (outletRepository.findById(outletId).isEmpty()) {
				throw new RuntimeException("Outlet not found with id: " + outletId);
			}
		}
	}

	private void requireMainPC(Long pcId, ProductionCenterType expected, String roleLabel) {
		if (pcId == null) {
			throw new RuntimeException("Production center is required for " + roleLabel + " role");
		}
		ProductionCenter pc = productionCenterRepository.findById(pcId)
				.orElseThrow(() -> new RuntimeException("Production center not found with id: " + pcId));
		if (pc.getType() != expected) {
			throw new RuntimeException("Production center type mismatch: expected " + expected
					+ ", got " + pc.getType());
		}
	}

	private void requireMpcInOutlet(Long mpcId, Long outletId) {
		if (mpcId == null) {
			throw new RuntimeException("MPC is required for MPC Worker role");
		}
		OutletProductionCenter mpc =
				outletProductionCenterRepository.findById(mpcId)
						.orElseThrow(() -> new RuntimeException("MPC not found with id: " + mpcId));
		Long mpcOutletId = mpc.getOutlet() != null ? mpc.getOutlet().getOutletId() : null;
		if (mpcOutletId == null || !mpcOutletId.equals(outletId)) {
			throw new RuntimeException("MPC does not belong to the specified outlet");
		}
	}

	private AdminUserDto toDto(BmsAuth u) {
		return AdminUserDto.builder()
				.id(IdUtil.bytesToUuidString(u.getId()))
				.firstName(u.getFirstName())
				.lastName(u.getLastName())
				.username(u.getUsername())
				.email(u.getEmail())
				.phone(u.getPhone())
				.roleId(u.getRoleId())
				.isActive(u.getIsActive())
				.verificationCode(u.getVerificationCode())
				.waiterId(u.getWaiterId())
				.outletId(u.getOutletId())
				.productionCenterId(u.getProductionCenterId())
				.mpcId(u.getMpcId())
				.build();
	}
}


