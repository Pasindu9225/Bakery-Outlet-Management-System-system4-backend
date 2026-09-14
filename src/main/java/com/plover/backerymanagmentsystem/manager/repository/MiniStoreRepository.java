package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.MiniStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MiniStoreRepository extends JpaRepository<MiniStore, Integer> {
    
    
    List<MiniStore> findAllByOrderByStoreDateDesc();

    java.util.Optional<MiniStore> findByName(String name);
}
