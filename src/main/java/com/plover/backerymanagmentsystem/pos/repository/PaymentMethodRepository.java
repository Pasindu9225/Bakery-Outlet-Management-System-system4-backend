package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.PaymentMethod;

/**
 * Repository interface for PaymentMethod entities
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

    /**
     * Find all payment methods ordered by name
     */
    List<PaymentMethod> findAllByOrderByNameAsc();
}
