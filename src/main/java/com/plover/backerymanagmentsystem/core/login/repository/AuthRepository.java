package com.plover.backerymanagmentsystem.core.login.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;

public interface AuthRepository extends JpaRepository<AuthModel, byte[]> {

    Optional<AuthModel> findByUsername(String username); // correct

    boolean existsByUsername(String username);           // (optional but useful)

    Optional<AuthModel> findByEmail(String email);       // (optional)

    Optional<AuthModel> findByPhone(String phone);       // (optional)

    Optional<AuthModel> findByVerificationCode(String verificationCode);
}
