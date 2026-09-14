package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByContactNumber(String contactNumber);
    Optional<Customer> findByIdCardNumber(String idCardNumber);
}
