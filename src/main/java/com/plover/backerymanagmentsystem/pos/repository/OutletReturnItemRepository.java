package com.plover.backerymanagmentsystem.pos.repository;

import com.plover.backerymanagmentsystem.pos.model.OutletReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutletReturnItemRepository extends JpaRepository<OutletReturnItem, Long> {
}
