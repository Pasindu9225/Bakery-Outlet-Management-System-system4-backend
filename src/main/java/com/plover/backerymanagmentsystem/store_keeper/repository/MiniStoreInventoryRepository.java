package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.store_keeper.repository.projection.MiniStoreInventoryProjection;

@Repository
public interface MiniStoreInventoryRepository extends JpaRepository<MiniStoreItem, Integer> {

    @Query(value = """
            SELECT
                msi.item_id AS item_id,
                msi.name AS name,
                msi.mini_store_id AS mini_store_id,
                ms.name AS mini_store_name,
                msi.raw_material_id AS raw_material_id,
                rm.material_name AS material_name,
                rm.material_code AS material_code,
                rm.unit_of_measure AS unit_of_measure,
                msi.product_id AS product_id,
                p.product_name AS product_name,
                p.product_code AS product_code,
                p.unit_price AS unit_price,
                msi.system_qty AS system_qty,
                msi.physical_qty AS physical_qty,
                msi.variance AS variance,
                msi.updated_at AS updated_at
            FROM mini_store_items msi
            JOIN mini_stores ms ON ms.mini_store_id = msi.mini_store_id
            LEFT JOIN raw_materials rm ON rm.id = msi.raw_material_id
            LEFT JOIN products p ON p.id = msi.product_id
            WHERE msi.outlet_id = :outletId
              AND msi.updated_at BETWEEN :startOfDay AND :endOfDay
            ORDER BY msi.mini_store_id, msi.item_id
            """, nativeQuery = true)
    List<MiniStoreInventoryProjection> findDailyInventory(
            @Param("outletId") Long outletId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
