package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.PosTable;

@Repository
public interface PosTableRepository extends JpaRepository<PosTable, Long> {
    List<PosTable> findAllByStatus(String status);
    Optional<PosTable> findByTableNameIgnoreCase(String tableName);
}
