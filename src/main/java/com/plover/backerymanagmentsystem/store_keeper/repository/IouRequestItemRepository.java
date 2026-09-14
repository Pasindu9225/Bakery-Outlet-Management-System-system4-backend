package com.plover.backerymanagmentsystem.store_keeper.repository;

import com.plover.backerymanagmentsystem.store_keeper.model.IouRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IouRequestItemRepository extends JpaRepository<IouRequestItem, Long> {
    List<IouRequestItem> findByIouRequestId(Long iouRequestId);
}
