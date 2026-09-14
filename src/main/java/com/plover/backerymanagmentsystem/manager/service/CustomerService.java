package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.model.Customer;
import com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory;
import java.util.List;
import java.util.Optional;

public interface CustomerService {
    Optional<Customer> findByContactNumber(String contactNumber);
    Customer registerCustomer(String name, String contactNumber, String idCardNumber);
    List<Customer> getAllCustomers();
    List<CustomerPointsHistory> getCustomerPointsHistory(Long customerId);
    void addPoints(Long customerId, Integer saleId, Double points, String description);
    void deductPoints(Long customerId, Integer saleId, Double points, String description);
    
    // OTP Management
    void sendOtp(String contactNumber);
    boolean verifyOtp(String contactNumber, String otp);
}
