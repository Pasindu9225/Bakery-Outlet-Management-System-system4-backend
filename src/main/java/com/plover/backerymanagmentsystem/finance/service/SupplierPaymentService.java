package com.plover.backerymanagmentsystem.finance.service;

import java.time.LocalDate;
import java.util.List;

import com.plover.backerymanagmentsystem.finance.dto.CreatePaymentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.OutstandingGrnDto;
import com.plover.backerymanagmentsystem.finance.dto.PaymentResponseDto;

/**
 * Service for the FR-FIN-03 settle-payments workflow. Owns the read of open
 * GRNs per supplier and the create/list/get of supplier payments.
 */
public interface SupplierPaymentService {

    /**
     * Return all GRNs for a supplier that still have an outstanding balance
     * (i.e. {@code grnStatus IN (RECEIVED, PARTIAL)} and the sum of allocated
     * payments is strictly less than the GRN total).
     *
     * @param supplierId the supplier id (must not be {@code null})
     * @return list of outstanding GRN projections, never {@code null}
     */
    List<OutstandingGrnDto> getOutstandingGrns(Long supplierId);

    /**
     * Create a new supplier payment with one or more allocations atomically.
     * Validates allocations against current outstanding balances, validates
     * payment date and method, generates a sequential payment reference, and
     * persists the header + lines in a single transaction.
     *
     * @param request the create-payment request
     * @return the persisted payment as a response dto
     */
    PaymentResponseDto createPayment(CreatePaymentRequestDto request);

    /**
     * List supplier payments with optional filters. All filters are
     * combined with AND.
     *
     * @param supplierId optional supplier filter
     * @param status     optional status filter ({@code Pending}/{@code Cleared})
     * @param from       optional inclusive start date
     * @param to         optional inclusive end date
     * @param search     optional case-insensitive substring matched against
     *                   payment ref, supplier name, allocated GRN refs and
     *                   remarks
     * @return matching payments, newest first
     */
    List<PaymentResponseDto> listPayments(Long supplierId, String status, LocalDate from, LocalDate to, String search);

    /**
     * Get a single payment by id.
     *
     * @param paymentId the payment id
     * @return the payment dto
     * @throws IllegalArgumentException if no such payment exists
     */
    PaymentResponseDto getPayment(Long paymentId);
}
