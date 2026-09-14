package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerPointsHistoryRepository extends JpaRepository<CustomerPointsHistory, Long> {
    List<CustomerPointsHistory> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
