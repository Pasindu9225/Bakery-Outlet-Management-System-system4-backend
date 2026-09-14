package com.plover.backerymanagmentsystem.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bmsauth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmsAuth {

	@Id
	@Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
	private byte[] id;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "email")
	private String email;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "phone")
	private String phone;

	@Column(name = "role_id")
	private String roleId;

	@Column(name = "username")
	private String username;

	@Column(name = "verification_code")
	private String verificationCode;

	@Column(name = "waiter_id", unique = true)
	private String waiterId;

	@Column(name = "outlet_id")
	private Long outletId;

	@Column(name = "production_center_id")
	private Long productionCenterId;

	@Column(name = "mpc_id")
	private Long mpcId;
}


