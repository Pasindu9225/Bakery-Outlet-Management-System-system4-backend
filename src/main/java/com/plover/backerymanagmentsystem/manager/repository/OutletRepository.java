package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.Outlet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, Long> {
    
    List<Outlet> findAllByOrderByNameAsc();
}
