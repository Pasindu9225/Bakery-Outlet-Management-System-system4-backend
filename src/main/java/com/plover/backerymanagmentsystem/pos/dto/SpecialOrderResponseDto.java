package com.plover.backerymanagmentsystem.pos.dto;

import com.plover.backerymanagmentsystem.pos.model.SpecialOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full response DTO for Special Order details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialOrderResponseDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerContact;
    private BigDecimal totalAmount;
    private BigDecimal advanceAmount;
    private BigDecimal balanceAmount;
    private LocalDate deliveryDate;
    private String notes;
    private SpecialOrderStatus status;
    private UUID cashierId;
    private String cashierName;
    private UUID approverId;
    private String approverName;
    private Long outletId;
    private String managerVerificationCode;
    private String verifiedByManagerName;
    private List<SpecialOrderItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
