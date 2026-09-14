package com.plover.backerymanagmentsystem.pos.service;

import com.plover.backerymanagmentsystem.pos.dto.CreateSpecialOrderRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.SpecialOrderPaymentRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.SpecialOrderResponseDto;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing Special Orders
 */
public interface SpecialOrderService {

    /**
     * Initiate a new special order with an initial advance payment
     */
    SpecialOrderResponseDto initiateOrder(CreateSpecialOrderRequestDto requestDto);

    /**
     * Get order details by ID
     */
    SpecialOrderResponseDto getOrderById(Long id);

    /**
     * Get all pending special orders
     */
    List<SpecialOrderResponseDto> getPendingOrders();

    /**
     * Search orders by delivery date
     */
    List<SpecialOrderResponseDto> getOrdersByDeliveryDate(LocalDate date);

    /**
     * Record a payment (advance or balance) against an order
     */
    SpecialOrderResponseDto recordPayment(Long id, SpecialOrderPaymentRequestDto requestDto);

    /**
     * Approve and close the order (manager approval)
     * This follows full payment and triggers inventory deduction and Sale creation.
     */
    SpecialOrderResponseDto approveAndCloseOrder(Long id, UUID managerId);
    
    /**
     * Cancel a special order
     */
    SpecialOrderResponseDto cancelOrder(Long id, String reason);

    /**
     * Verify manager verification code
     */
    AuthModel verifyManagerCode(String code);
}
