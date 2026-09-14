package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;

import com.plover.backerymanagmentsystem.pos.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    
    List<Discount> findByIsActiveTrue();
    
    boolean existsByName(String name);
    java.util.Optional<Discount> findByName(String name);
    
    @Query("SELECT d FROM Discount d LEFT JOIN d.applicableProducts p " +
           "WHERE d.isActive = true AND (d.appliedToAllProducts = true OR p.id = :productId)")
    List<Discount> findActiveDiscountsByProduct(@Param("productId") Long productId);
}
