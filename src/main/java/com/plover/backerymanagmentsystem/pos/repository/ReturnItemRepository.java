package com.plover.backerymanagmentsystem.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.ReturnItem;
import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Integer> {
    List<ReturnItem> findBySaleItemId(Integer saleItemId);
}
