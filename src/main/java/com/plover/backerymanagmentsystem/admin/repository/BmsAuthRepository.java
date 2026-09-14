package com.plover.backerymanagmentsystem.admin.repository;

import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BmsAuthRepository extends JpaRepository<BmsAuth, byte[]> {

    long countByOutletId(Long outletId);
    java.util.List<BmsAuth> findByRoleIdAndOutletId(String roleId, Long outletId);
    java.util.Optional<BmsAuth> findByVerificationCode(String verificationCode);
}


