package com.plover.backerymanagmentsystem.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.DayEndClosingItem;

@Repository
public interface DayEndClosingItemRepository extends JpaRepository<DayEndClosingItem, Integer> {
    java.util.List<DayEndClosingItem> findByDayEndClosing_Id(Integer closingId);
}
