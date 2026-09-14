package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.plover.backerymanagmentsystem.manager.model.TransferNote;

@Repository
public interface TransferNoteRepository extends JpaRepository<TransferNote, Long> {
    List<TransferNote> findBySourceProductionCenterIdOrderByCreatedAtDesc(Long pcId);
}
