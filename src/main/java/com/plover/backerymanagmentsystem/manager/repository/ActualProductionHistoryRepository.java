package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistory;
import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistoryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActualProductionHistoryRepository extends JpaRepository<ActualProductionHistory, Long> {
    List<ActualProductionHistory> findByActionTypeOrderByCreatedAtDesc(ActualProductionHistoryAction actionType);
    boolean existsByProductIdAndReferenceNameAndActionType(Long productId, String referenceName, ActualProductionHistoryAction actionType);
}
