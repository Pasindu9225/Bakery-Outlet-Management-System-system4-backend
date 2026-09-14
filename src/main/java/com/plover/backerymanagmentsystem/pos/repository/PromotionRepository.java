package com.plover.backerymanagmentsystem.pos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.Promotion;

/**
 * Repository interface for Promotion entity operations.
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    /**
     * Finds a promotion by its promo code, ignoring case.
     * 
     * @param promoCode the promo code to search for
     * @return an Optional containing the Promotion if found
     */
    Optional<Promotion> findByPromoCodeIgnoreCase(String promoCode);
}
