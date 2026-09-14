package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full supplier profile + counts for the FR-MIS-02 supplier-detail modal.
 *
 * <p>Mirrors {@link MisSupplierListItemDto} but adds extra master-data
 * fields ({@code repName}, {@code repContactNo}, {@code bankDetails},
 * {@code vatStatus}) for the profile section.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisSupplierDetailDto {

    private String id;

    private Long supplierId;

    private String name;

    private String phone;

    private String email;

    private String address;

    private String status;

    private BigDecimal outstandingBalance;

    private boolean overdue;

    private LocalDate lastTransactionDate;

    private long totalPos;

    private long totalGrns;

    private String registrationId;

    private LocalDate registrationDate;

    private String repName;

    private String repContactNo;

    private String bankDetails;

    private String vatStatus;
}
