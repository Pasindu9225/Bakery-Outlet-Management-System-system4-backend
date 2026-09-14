package com.plover.backerymanagmentsystem.manager.controller;

import com.plover.backerymanagmentsystem.manager.model.Customer;
import com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory;
import com.plover.backerymanagmentsystem.manager.service.CustomerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/lookup")
    public ResponseEntity<Customer> lookupCustomer(@RequestParam String phone) {
        log.info("Received request to lookup customer by phone: {}", phone);
        return customerService.findByContactNumber(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<Customer> registerCustomer(@RequestBody CustomerRegistrationDto registrationDto) {
        log.info("Received request to register customer: {}", registrationDto.getContactNumber());
        Customer customer = customerService.registerCustomer(
                registrationDto.getName(),
                registrationDto.getContactNumber(),
                registrationDto.getIdCardNumber()
        );
        return ResponseEntity.ok(customer);
    }

    @GetMapping("")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        log.info("Received request to get all customers");
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}/points-history")
    public ResponseEntity<List<CustomerPointsHistory>> getPointsHistory(@PathVariable Long id) {
        log.info("Received request to get points history for customer ID: {}", id);
        return ResponseEntity.ok(customerService.getCustomerPointsHistory(id));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody OtpRequestDto otpRequest) {
        log.info("Received request to send OTP to: {}", otpRequest.getPhone());
        customerService.sendOtp(otpRequest.getPhone());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP sent successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody OtpVerifyDto otpVerify) {
        log.info("Received request to verify OTP for: {}", otpVerify.getPhone());
        boolean verified = customerService.verifyOtp(otpVerify.getPhone(), otpVerify.getOtp());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", verified);
        response.put("message", verified ? "OTP verified successfully." : "Invalid or expired OTP.");
        
        if (verified) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Data
    public static class CustomerRegistrationDto {
        private String name;
        private String contactNumber;
        private String idCardNumber;
    }

    @Data
    public static class OtpRequestDto {
        private String phone;
    }

    @Data
    public static class OtpVerifyDto {
        private String phone;
        private String otp;
    }
}
