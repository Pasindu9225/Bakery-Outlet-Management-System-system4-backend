package com.plover.backerymanagmentsystem.store_keeper.repository;

import com.plover.backerymanagmentsystem.store_keeper.model.IouRequest;
import com.plover.backerymanagmentsystem.store_keeper.model.IouStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IouRequestRepository extends JpaRepository<IouRequest, Long> {
    List<IouRequest> findByStatusOrderByCreatedAtDesc(IouStatus status);
    List<IouRequest> findAllByOrderByCreatedAtDesc();
}
