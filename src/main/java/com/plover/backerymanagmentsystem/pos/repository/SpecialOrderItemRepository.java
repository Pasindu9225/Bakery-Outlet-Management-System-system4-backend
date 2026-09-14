package com.plover.backerymanagmentsystem.pos.repository;

import com.plover.backerymanagmentsystem.pos.model.SpecialOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialOrderItemRepository extends JpaRepository<SpecialOrderItem, Long> {
    List<SpecialOrderItem> findBySpecialOrderId(Long specialOrderId);
}
