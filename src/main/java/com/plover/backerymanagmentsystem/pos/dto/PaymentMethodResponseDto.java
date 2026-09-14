package com.plover.backerymanagmentsystem.pos.dto;

import com.plover.backerymanagmentsystem.pos.model.PaymentMethod;

/**
 * Response DTO for payment method information
 */
public class PaymentMethodResponseDto {

    private Integer paymentMethodId;
    private String name;

    public PaymentMethodResponseDto() {
    }

    public PaymentMethodResponseDto(Integer paymentMethodId, String name) {
        this.paymentMethodId = paymentMethodId;
        this.name = name;
    }

    // Getters and Setters
    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Factory method to create DTO from entity
     */
    public static PaymentMethodResponseDto fromEntity(PaymentMethod paymentMethod) {
        return new PaymentMethodResponseDto(
                paymentMethod.getPaymentMethodId(),
                paymentMethod.getName()
        );
    }
}
