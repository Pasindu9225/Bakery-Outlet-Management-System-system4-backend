package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByProductName(String productName);
    
    Optional<Product> findByProductCode(String productCode);
    
    List<Product> findByIsActiveTrue();
    
    @Query("SELECT p FROM Product p JOIN p.categoryRef c WHERE c.name = :category")
    List<Product> findByCategory(@Param("category") String category);
    
    @Query("SELECT p FROM Product p WHERE p.productName LIKE %:searchTerm% OR p.description LIKE %:searchTerm%")
    List<Product> findBySearchTerm(@Param("searchTerm") String searchTerm);
}
