package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;

import com.plover.backerymanagmentsystem.pos.dto.PaymentMethodResponseDto;

public interface PaymentMethodService {

    /**
     * Get all payment methods ordered by name
     *
     * @return List of all payment methods
     */
    List<PaymentMethodResponseDto> getAllPaymentMethods();
}
