package com.plover.backerymanagmentsystem.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.RecipeIngredient;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
}


