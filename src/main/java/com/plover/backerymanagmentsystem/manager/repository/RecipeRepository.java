package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    
    Optional<Recipe> findByProductIdAndIsActiveTrue(Long productId);
    
    List<Recipe> findByProductId(Long productId);
    
    List<Recipe> findByIsActiveTrue();
    
    @Query("SELECT r FROM Recipe r JOIN r.product p WHERE p.productName LIKE %:productName% AND r.isActive = true")
    List<Recipe> findByProductNameContaining(@Param("productName") String productName);
}
