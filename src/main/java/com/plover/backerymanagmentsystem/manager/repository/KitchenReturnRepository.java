package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.plover.backerymanagmentsystem.manager.model.KitchenReturn;

@Repository
public interface KitchenReturnRepository extends JpaRepository<KitchenReturn, Long> {
    List<KitchenReturn> findBySourceProductionCenterIdOrderByCreatedAtDesc(Long pcId);
}
