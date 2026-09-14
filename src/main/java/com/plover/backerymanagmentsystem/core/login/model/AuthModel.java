package com.plover.backerymanagmentsystem.core.login.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bmsauth")
public class AuthModel {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash; 

    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "role_id")
    private String roleId;    

    @Column(name = "is_active")
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

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
