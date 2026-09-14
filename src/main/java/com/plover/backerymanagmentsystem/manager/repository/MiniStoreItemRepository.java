package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MiniStoreItemRepository extends JpaRepository<MiniStoreItem, Integer> {
    
    List<MiniStoreItem> findByMiniStore_MiniStoreId(Integer miniStoreId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MiniStoreItem msi WHERE msi.miniStore.miniStoreId = :miniStoreId")
    void deleteByMiniStoreId(@Param("miniStoreId") Integer miniStoreId);

    java.util.Optional<MiniStoreItem> findByProductIdAndOutletId(Long productId, Long outletId);
}
