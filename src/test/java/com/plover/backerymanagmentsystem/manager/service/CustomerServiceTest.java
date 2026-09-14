package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.model.Customer;
import com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory;
import com.plover.backerymanagmentsystem.manager.repository.CustomerPointsHistoryRepository;
import com.plover.backerymanagmentsystem.manager.repository.CustomerRepository;
import com.plover.backerymanagmentsystem.manager.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerPointsHistoryRepository pointsHistoryRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRegisterCustomer_Success() {
        String name = "John Doe";
        String contact = "0771234567";
        String idCard = "123456789V";

        when(customerRepository.findByContactNumber(contact)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer registered = customerService.registerCustomer(name, contact, idCard);

        assertNotNull(registered);
        assertEquals(name, registered.getName());
        assertEquals(contact, registered.getContactNumber());
        assertEquals(idCard, registered.getIdCardNumber());
        assertEquals(0.0, registered.getLoyaltyPoints());

        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(pointsHistoryRepository, times(1)).save(any(CustomerPointsHistory.class));
    }

    @Test
    public void testRegisterCustomer_WithoutIdCard_Success() {
        String name = "John Doe";
        String contact = "0771234567";

        when(customerRepository.findByContactNumber(contact)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer registered = customerService.registerCustomer(name, contact, null);

        assertNotNull(registered);
        assertEquals(name, registered.getName());
        assertEquals(contact, registered.getContactNumber());
        assertNull(registered.getIdCardNumber());
        assertEquals(0.0, registered.getLoyaltyPoints());

        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(pointsHistoryRepository, times(1)).save(any(CustomerPointsHistory.class));
    }

    @Test
    public void testRegisterCustomer_InvalidPhone_ThrowsException() {
        String name = "John Doe";
        String contact = "077123";
        String idCard = "123456789V";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.registerCustomer(name, contact, idCard);
        });

        assertTrue(exception.getMessage().contains("exactly 10 digits"));
    }

    @Test
    public void testRegisterCustomer_InvalidNic_ThrowsException() {
        String name = "John Doe";
        String contact = "0771234567";
        String idCard = "123456";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.registerCustomer(name, contact, idCard);
        });

        assertTrue(exception.getMessage().contains("Invalid Sri Lankan NIC number"));
    }

    @Test
    public void testRegisterCustomer_DuplicateContact_ThrowsException() {
        String name = "John Doe";
        String contact = "0771234567";
        String idCard = "123456789V";

        Customer existing = Customer.builder().contactNumber(contact).build();
        when(customerRepository.findByContactNumber(contact)).thenReturn(Optional.of(existing));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            customerService.registerCustomer(name, contact, idCard);
        });

        assertTrue(exception.getMessage().contains("already registered"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    public void testRegisterCustomer_DuplicateNic_ThrowsException() {
        String name = "John Doe";
        String contact = "0771234567";
        String idCard = "123456789V";

        Customer existing = Customer.builder().idCardNumber(idCard).build();
        when(customerRepository.findByContactNumber(contact)).thenReturn(Optional.empty());
        when(customerRepository.findByIdCardNumber(idCard)).thenReturn(Optional.of(existing));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            customerService.registerCustomer(name, contact, idCard);
        });

        assertTrue(exception.getMessage().contains("already registered"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    public void testAddPoints_Success() {
        Long customerId = 1L;
        Customer customer = Customer.builder().id(customerId).loyaltyPoints(10.5).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerService.addPoints(customerId, 100, 5.0, "Earned from sale");

        assertEquals(15.5, customer.getLoyaltyPoints());
        verify(customerRepository, times(1)).save(customer);

        ArgumentCaptor<CustomerPointsHistory> historyCaptor = ArgumentCaptor.forClass(CustomerPointsHistory.class);
        verify(pointsHistoryRepository, times(1)).save(historyCaptor.capture());
        assertEquals(5.0, historyCaptor.getValue().getPointsChanged());
        assertEquals("Earned from sale", historyCaptor.getValue().getDescription());
        assertEquals(100, historyCaptor.getValue().getSaleId());
    }

    @Test
    public void testDeductPoints_Success() {
        Long customerId = 1L;
        Customer customer = Customer.builder().id(customerId).loyaltyPoints(1200.0).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerService.deductPoints(customerId, 101, 1000.0, "Redeemed points");

        assertEquals(200.0, customer.getLoyaltyPoints());
        verify(customerRepository, times(1)).save(customer);

        ArgumentCaptor<CustomerPointsHistory> historyCaptor = ArgumentCaptor.forClass(CustomerPointsHistory.class);
        verify(pointsHistoryRepository, times(1)).save(historyCaptor.capture());
        assertEquals(-1000.0, historyCaptor.getValue().getPointsChanged());
        assertEquals("Redeemed points", historyCaptor.getValue().getDescription());
    }

    @Test
    public void testDeductPoints_InsufficientPoints_ThrowsException() {
        Long customerId = 1L;
        Customer customer = Customer.builder().id(customerId).loyaltyPoints(500.0).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            customerService.deductPoints(customerId, 102, 1000.0, "Redeemed points");
        });

        assertTrue(exception.getMessage().contains("Insufficient loyalty points"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    public void testOtpFlow() {
        String phone = "0771234567";
        
        // Test normal verification - should fail before sending
        assertFalse(customerService.verifyOtp(phone, "111111"));

        // Send OTP
        customerService.sendOtp(phone);

        // Bypass OTP should always work
        assertTrue(customerService.verifyOtp(phone, "123456"));
    }
}
