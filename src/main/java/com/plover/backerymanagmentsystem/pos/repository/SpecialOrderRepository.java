package com.plover.backerymanagmentsystem.pos.repository;

import com.plover.backerymanagmentsystem.pos.model.SpecialOrder;
import com.plover.backerymanagmentsystem.pos.model.SpecialOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpecialOrderRepository extends JpaRepository<SpecialOrder, Long> {
    List<SpecialOrder> findByStatus(SpecialOrderStatus status);
    List<SpecialOrder> findByDeliveryDate(LocalDate deliveryDate);
}
