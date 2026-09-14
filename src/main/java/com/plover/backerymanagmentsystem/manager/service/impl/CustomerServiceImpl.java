package com.plover.backerymanagmentsystem.manager.service.impl;

import com.plover.backerymanagmentsystem.manager.model.Customer;
import com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory;
import com.plover.backerymanagmentsystem.manager.repository.CustomerRepository;
import com.plover.backerymanagmentsystem.manager.repository.CustomerPointsHistoryRepository;
import com.plover.backerymanagmentsystem.manager.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPointsHistoryRepository pointsHistoryRepository;
    private final HutchSmsService hutchSmsService;

    // In-memory cache for OTP codes. Key = Contact Number, Value = OTP Code
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpiryCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findByContactNumber(String contactNumber) {
        log.info("Looking up customer by contact number: {}", contactNumber);
        return customerRepository.findByContactNumber(contactNumber);
    }

    @Override
    @Transactional
    public Customer registerCustomer(String name, String contactNumber, String idCardNumber) {
        log.info("Registering new customer. Name: {}, Contact: {}, ID Card: {}", name, contactNumber, idCardNumber);
        
        if (contactNumber == null || !contactNumber.trim().matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
        }
        
        if (idCardNumber != null && !idCardNumber.trim().isEmpty() && !isValidNic(idCardNumber)) {
            throw new IllegalArgumentException("Invalid Sri Lankan NIC number format.");
        }

        Optional<Customer> existing = customerRepository.findByContactNumber(contactNumber.trim());
        if (existing.isPresent()) {
            throw new RuntimeException("Customer with contact number " + contactNumber + " is already registered.");
        }

        if (idCardNumber != null && !idCardNumber.trim().isEmpty()) {
            Optional<Customer> existingNic = customerRepository.findByIdCardNumber(idCardNumber.trim());
            if (existingNic.isPresent()) {
                throw new RuntimeException("Customer with ID Card Number " + idCardNumber.trim() + " is already registered.");
            }
        }

        Customer customer = Customer.builder()
                .name(name)
                .contactNumber(contactNumber.trim())
                .idCardNumber(idCardNumber != null ? idCardNumber.trim() : null)
                .loyaltyPoints(0.0)
                .isCreditAllowed(false)
                .build();

        Customer saved = customerRepository.save(customer);

        // Add initial history record
        CustomerPointsHistory history = CustomerPointsHistory.builder()
                .customer(saved)
                .pointsChanged(0.0)
                .description("Customer registered")
                .build();
        pointsHistoryRepository.save(history);

        return saved;
    }

    private boolean isValidNic(String nicNumber) {
        if (nicNumber == null || nicNumber.trim().isEmpty()) {
            return true;
        }
        String nic = nicNumber.trim().toUpperCase();
        String yearStr = "";
        String dayStr = "";
        
        if (nic.length() == 10 && nic.substring(0, 9).matches("\\d{9}") && (nic.endsWith("V") || nic.endsWith("X"))) {
            yearStr = "19" + nic.substring(0, 2);
            dayStr = nic.substring(2, 5);
        } else if (nic.length() == 12 && nic.matches("\\d{12}")) {
            yearStr = nic.substring(0, 4);
            dayStr = nic.substring(4, 7);
        } else {
            return false;
        }
        
        try {
            int year = Integer.parseInt(yearStr);
            int dayValue = Integer.parseInt(dayStr);
            
            int daysToAdd = dayValue;
            if (dayValue > 500) {
                daysToAdd = dayValue - 500;
            }
            
            if (daysToAdd < 1 || daysToAdd > 366) {
                return false;
            }
            
            int[] monthLengths = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            int remainingDays = daysToAdd;
            int monthIndex = 0;
            while (monthIndex < 12 && remainingDays > monthLengths[monthIndex]) {
                remainingDays -= monthLengths[monthIndex];
                monthIndex++;
            }
            
            return monthIndex < 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        log.info("Fetching all customers");
        return customerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerPointsHistory> getCustomerPointsHistory(Long customerId) {
        log.info("Fetching points history for customer ID: {}", customerId);
        return pointsHistoryRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional
    public void addPoints(Long customerId, Integer saleId, Double points, String description) {
        log.info("Adding {} points to customer ID: {}. Sale: {}", points, customerId, saleId);
        if (points == null || points <= 0) {
            return;
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        Double currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0.0;
        customer.setLoyaltyPoints(currentPoints + points);
        customerRepository.save(customer);

        CustomerPointsHistory history = CustomerPointsHistory.builder()
                .customer(customer)
                .saleId(saleId)
                .pointsChanged(points)
                .description(description)
                .build();
        pointsHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void deductPoints(Long customerId, Integer saleId, Double points, String description) {
        log.info("Deducting {} points from customer ID: {}. Sale: {}", points, customerId, saleId);
        if (points == null || points <= 0) {
            return;
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        Double currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0.0;
        if (currentPoints < points) {
            throw new RuntimeException("Insufficient loyalty points. Available: " + currentPoints + ", Requested: " + points);
        }

        customer.setLoyaltyPoints(currentPoints - points);
        customerRepository.save(customer);

        CustomerPointsHistory history = CustomerPointsHistory.builder()
                .customer(customer)
                .saleId(saleId)
                .pointsChanged(-points)
                .description(description)
                .build();
        pointsHistoryRepository.save(history);
    }

    @Override
    public void sendOtp(String contactNumber) {
        log.info("Generating OTP for customer contact number: {}", contactNumber);
        
        // Generate a random 6-digit OTP
        int otpCode = 100000 + new Random().nextInt(900000);
        String otpStr = String.valueOf(otpCode);

        // Save in cache (5 minutes expiration)
        otpCache.put(contactNumber, otpStr);
        otpExpiryCache.put(contactNumber, LocalDateTime.now().plusMinutes(5));

        // Print/log the OTP clearly for dev environment
        log.info("\n==================================================");
        log.info("[SMS GATEWAY] Sending OTP {} to phone number {}", otpStr, contactNumber);
        log.info("==================================================\n");

        // Dispatch SMS via Hutch Gateway
        String smsMessage = "Your Bakery Outlet registration OTP code is: " + otpStr + ". Valid for 5 minutes.";
        hutchSmsService.sendSms(contactNumber, smsMessage);
    }

    @Override
    public boolean verifyOtp(String contactNumber, String otp) {
        log.info("Verifying OTP for customer: {}", contactNumber);
        
        if (otp == null || otp.trim().isEmpty()) {
            return false;
        }

        // Test/bypass OTP
        if ("123456".equals(otp)) {
            log.info("Bypass OTP '123456' accepted for customer {}", contactNumber);
            return true;
        }

        String cachedOtp = otpCache.get(contactNumber);
        LocalDateTime expiry = otpExpiryCache.get(contactNumber);

        if (cachedOtp == null || expiry == null) {
            log.warn("No OTP found for customer {}", contactNumber);
            return false;
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            log.warn("OTP has expired for customer {}", contactNumber);
            otpCache.remove(contactNumber);
            otpExpiryCache.remove(contactNumber);
            return false;
        }

        boolean isValid = cachedOtp.equals(otp);
        if (isValid) {
            log.info("OTP verified successfully for customer {}", contactNumber);
            otpCache.remove(contactNumber);
            otpExpiryCache.remove(contactNumber);
        } else {
            log.warn("Invalid OTP entered for customer {}", contactNumber);
        }

        return isValid;
    }
}
