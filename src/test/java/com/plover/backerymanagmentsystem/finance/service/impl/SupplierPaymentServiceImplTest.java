package com.plover.backerymanagmentsystem.finance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.finance.dto.CreatePaymentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.CreatePaymentRequestDto.AllocationDto;
import com.plover.backerymanagmentsystem.finance.dto.OutstandingGrnDto;
import com.plover.backerymanagmentsystem.finance.dto.PaymentResponseDto;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentAllocationRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class SupplierPaymentServiceImplTest {

    @Mock private SupplierPaymentRepository paymentRepository;
    @Mock private SupplierPaymentAllocationRepository allocationRepository;
    @Mock private GrnRepository grnRepository;
    @Mock private SupplierRepository supplierRepository;

    @InjectMocks private SupplierPaymentServiceImpl service;

    private static final Long SUP = 1L;

    private Grn grn(Long id, LocalDate date, BigDecimal total, GrnStatus status) {
        return Grn.builder()
                .grnId(id)
                .receivedDate(date.atStartOfDay())
                .poId(100L + id)
                .supplierId(SUP)
                .total(total)
                .grnStatus(status)
                .invoiceNumber("INV-" + id)
                .build();
    }

    private CreatePaymentRequestDto request(LocalDate date, String method, AllocationDto... allocs) {
        return CreatePaymentRequestDto.builder()
                .supplierId(SUP)
                .paymentDate(date)
                .paymentMethod(method)
                .remarks("Test payment")
                .allocations(List.of(allocs))
                .build();
    }

    private AllocationDto alloc(Long grnId, String amount) {
        return AllocationDto.builder().grnId(grnId).allocatedAmount(new BigDecimal(amount)).build();
    }

    private void mockSupplierName() {
        lenient().when(supplierRepository.findById(SUP))
                .thenReturn(Optional.of(Supplier.builder().supplierId(SUP).name("Fresh Farms Ltd").build()));
    }

    private void mockSaveEchoes() {
        when(paymentRepository.save(any(SupplierPayment.class))).thenAnswer(inv -> {
            SupplierPayment p = inv.getArgument(0);
            p.setPaymentId(99L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });
    }

    // -- createPayment ---------------------------------------------------

    @Test
    void createPayment_singleAllocationSucceeds() {
        Grn g = grn(5L, LocalDate.now().minusDays(3), new BigDecimal("1000.00"), GrnStatus.RECEIVED);
        when(grnRepository.findAllById(List.of(5L))).thenReturn(List.of(g));
        when(allocationRepository.sumAllocatedByGrnId(5L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByPaymentRefPrefix(anyString())).thenReturn(0L);
        mockSupplierName();
        mockSaveEchoes();

        PaymentResponseDto response = service.createPayment(
                request(LocalDate.now(), "Bank Transfer", alloc(5L, "400.00")));

        assertThat(response.getPaymentId()).isEqualTo(99L);
        assertThat(response.getPaymentRef()).startsWith("PMT-");
        assertThat(response.getAmount()).isEqualByComparingTo("400.00");
        assertThat(response.getPaymentMethod()).isEqualTo("Bank Transfer");
        assertThat(response.getStatus()).isEqualTo("Cleared");
        assertThat(response.getSupplierName()).isEqualTo("Fresh Farms Ltd");
        assertThat(response.getAllocations()).hasSize(1);
        assertThat(response.getAllocations().get(0).getGrnId()).isEqualTo(5L);
        assertThat(response.getAllocations().get(0).getGrnRef()).isEqualTo("GRN-5");
        assertThat(response.getAllocations().get(0).getAllocatedAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void createPayment_partialAllocationLeavesOutstanding() {
        Grn g = grn(5L, LocalDate.now().minusDays(2), new BigDecimal("1000.00"), GrnStatus.RECEIVED);
        when(grnRepository.findAllById(List.of(5L))).thenReturn(List.of(g));
        // Already paid 600 → outstanding 400, allocate 200 → ok
        when(allocationRepository.sumAllocatedByGrnId(5L)).thenReturn(new BigDecimal("600.00"));
        when(paymentRepository.countByPaymentRefPrefix(anyString())).thenReturn(2L);
        mockSupplierName();
        mockSaveEchoes();

        PaymentResponseDto response = service.createPayment(
                request(LocalDate.now(), "CASH", alloc(5L, "200.00")));

        assertThat(response.getAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getPaymentMethod()).isEqualTo("Cash");
        assertThat(response.getPaymentRef()).endsWith("-003"); // count=2 → next 003
    }

    @Test
    void createPayment_amountExceedingOutstandingRejected() {
        Grn g = grn(5L, LocalDate.now().minusDays(1), new BigDecimal("500.00"), GrnStatus.RECEIVED);
        when(grnRepository.findAllById(List.of(5L))).thenReturn(List.of(g));
        when(allocationRepository.sumAllocatedByGrnId(5L)).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now(), "BANK_TRANSFER", alloc(5L, "401.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds outstanding");
    }

    @Test
    void createPayment_dateMoreThan30DaysInFutureRejected() {
        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now().plusDays(31), "BANK_TRANSFER", alloc(5L, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void createPayment_dateBeforeEarliestGrnReceiptRejected() {
        Grn g = grn(5L, LocalDate.now().minusDays(2), new BigDecimal("500.00"), GrnStatus.RECEIVED);
        when(grnRepository.findAllById(List.of(5L))).thenReturn(List.of(g));
        when(allocationRepository.sumAllocatedByGrnId(5L)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now().minusDays(10), "BANK_TRANSFER", alloc(5L, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before the earliest");
    }

    @Test
    void createPayment_emptyAllocationsRejected() {
        CreatePaymentRequestDto req = CreatePaymentRequestDto.builder()
                .supplierId(SUP)
                .paymentDate(LocalDate.now())
                .paymentMethod("BANK_TRANSFER")
                .allocations(Collections.emptyList())
                .build();

        assertThatThrownBy(() -> service.createPayment(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocation");
    }

    @Test
    void createPayment_invalidPaymentMethodRejected() {
        // No grn lookup happens because method validation fails first.
        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now(), "CRYPTO", alloc(5L, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMethod");
    }

    @Test
    void createPayment_grnFromDifferentSupplierRejected() {
        Grn g = Grn.builder()
                .grnId(5L)
                .receivedDate(LocalDate.now().minusDays(2).atStartOfDay())
                .poId(105L)
                .supplierId(99L) // not SUP
                .total(new BigDecimal("500"))
                .grnStatus(GrnStatus.RECEIVED)
                .build();
        when(grnRepository.findAllById(List.of(5L))).thenReturn(List.of(g));

        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now(), "CASH", alloc(5L, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void createPayment_pendingGrnRejected() {
        Grn g = grn(5L, LocalDate.now().minusDays(2), new BigDecimal("500.00"), GrnStatus.PENDING);
        when(grnRepository.findAllById(List.of(5L))).thenReturn(List.of(g));

        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now(), "CASH", alloc(5L, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not eligible");
    }

    @Test
    void createPayment_unknownGrnRejected() {
        when(grnRepository.findAllById(List.of(99L))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.createPayment(
                request(LocalDate.now(), "CASH", alloc(99L, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void createPayment_multipleAllocationsTotalAmount() {
        Grn g1 = grn(5L, LocalDate.now().minusDays(5), new BigDecimal("500.00"), GrnStatus.RECEIVED);
        Grn g2 = grn(6L, LocalDate.now().minusDays(3), new BigDecimal("700.00"), GrnStatus.RECEIVED);
        when(grnRepository.findAllById(List.of(5L, 6L))).thenReturn(List.of(g1, g2));
        when(allocationRepository.sumAllocatedByGrnId(5L)).thenReturn(BigDecimal.ZERO);
        when(allocationRepository.sumAllocatedByGrnId(6L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByPaymentRefPrefix(anyString())).thenReturn(0L);
        mockSupplierName();
        mockSaveEchoes();

        PaymentResponseDto response = service.createPayment(
                request(LocalDate.now(), "ONLINE_TRANSFER",
                        alloc(5L, "300.00"), alloc(6L, "200.00")));

        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
        assertThat(response.getAllocations()).hasSize(2);
        assertThat(response.getPaymentMethod()).isEqualTo("Online Transfer");
    }

    // -- getOutstandingGrns ---------------------------------------------

    @Test
    void getOutstandingGrns_excludesFullyPaidAndPendingGrns() {
        Grn fullyPaid = grn(1L, LocalDate.now().minusDays(10), new BigDecimal("100.00"), GrnStatus.RECEIVED);
        Grn outstanding = grn(2L, LocalDate.now().minusDays(5), new BigDecimal("200.00"), GrnStatus.PARTIAL);
        Grn pendingGrn = grn(3L, LocalDate.now().minusDays(2), new BigDecimal("50.00"), GrnStatus.PENDING);
        Grn cancelled = grn(4L, LocalDate.now().minusDays(2), new BigDecimal("80.00"), GrnStatus.CANCELLED);
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(fullyPaid, outstanding, pendingGrn, cancelled));
        when(allocationRepository.sumAllocatedByGrnId(1L)).thenReturn(new BigDecimal("100.00"));
        when(allocationRepository.sumAllocatedByGrnId(2L)).thenReturn(new BigDecimal("50.00"));

        List<OutstandingGrnDto> result = service.getOutstandingGrns(SUP);

        assertThat(result).hasSize(1);
        OutstandingGrnDto only = result.get(0);
        assertThat(only.getGrnId()).isEqualTo(2L);
        assertThat(only.getRef()).isEqualTo("GRN-2");
        assertThat(only.getAmount()).isEqualByComparingTo("200.00");
        assertThat(only.getPaid()).isEqualByComparingTo("50.00");
        assertThat(only.getOutstanding()).isEqualByComparingTo("150.00");
        assertThat(only.getStatus()).isEqualTo("Pending");
        assertThat(only.getDescription()).isEqualTo("Goods Received - INV-2");
    }

    @Test
    void getOutstandingGrns_marksOverdueWhenPastDueDate() {
        Grn old = grn(7L, LocalDate.now().minusDays(45), new BigDecimal("300.00"), GrnStatus.RECEIVED);
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(old));
        when(allocationRepository.sumAllocatedByGrnId(7L)).thenReturn(BigDecimal.ZERO);

        List<OutstandingGrnDto> result = service.getOutstandingGrns(SUP);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("Overdue");
    }

    @Test
    void getOutstandingGrns_nullSupplierIdRejected() {
        assertThatThrownBy(() -> service.getOutstandingGrns(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supplierId");
    }

    // -- listPayments ---------------------------------------------------

    @Test
    void listPayments_filtersByStatusAndDateRange() {
        SupplierPayment p1 = SupplierPayment.builder()
                .paymentId(1L).paymentRef("PMT-2026-001")
                .paymentDate(LocalDate.of(2026, 1, 10))
                .supplierId(SUP).amount(new BigDecimal("100"))
                .paymentMethod("BANK_TRANSFER").status("CLEARED")
                .allocations(Collections.emptyList()).build();
        SupplierPayment p2 = SupplierPayment.builder()
                .paymentId(2L).paymentRef("PMT-2026-002")
                .paymentDate(LocalDate.of(2026, 3, 20))
                .supplierId(SUP).amount(new BigDecimal("200"))
                .paymentMethod("CASH").status("PENDING")
                .allocations(Collections.emptyList()).build();
        SupplierPayment p3 = SupplierPayment.builder()
                .paymentId(3L).paymentRef("PMT-2026-003")
                .paymentDate(LocalDate.of(2026, 5, 1))
                .supplierId(SUP).amount(new BigDecimal("300"))
                .paymentMethod("CHEQUE").status("CLEARED")
                .allocations(Collections.emptyList()).build();
        when(paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc())
                .thenReturn(List.of(p3, p2, p1));
        mockSupplierName();

        // status=Cleared returns p1 and p3
        List<PaymentResponseDto> cleared = service.listPayments(null, "Cleared", null, null, null);
        assertThat(cleared).hasSize(2);
        assertThat(cleared).extracting(PaymentResponseDto::getPaymentId).containsExactly(3L, 1L);

        // date range Feb-Apr returns only p2
        List<PaymentResponseDto> midRange = service.listPayments(null, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30), null);
        assertThat(midRange).hasSize(1);
        assertThat(midRange.get(0).getPaymentId()).isEqualTo(2L);

        // supplier filter passes
        List<PaymentResponseDto> bySupplier = service.listPayments(SUP, null, null, null, null);
        assertThat(bySupplier).hasSize(3);

        // supplier filter no-match returns empty
        List<PaymentResponseDto> none = service.listPayments(999L, null, null, null, null);
        assertThat(none).isEmpty();
    }

    @Test
    void listPayments_searchMatchesPaymentRef() {
        SupplierPayment p1 = SupplierPayment.builder()
                .paymentId(1L).paymentRef("PMT-2026-001")
                .paymentDate(LocalDate.now()).supplierId(SUP)
                .amount(new BigDecimal("100")).paymentMethod("BANK_TRANSFER")
                .status("CLEARED").remarks("Aug batch")
                .allocations(Collections.emptyList()).build();
        SupplierPayment p2 = SupplierPayment.builder()
                .paymentId(2L).paymentRef("PMT-2026-077")
                .paymentDate(LocalDate.now()).supplierId(SUP)
                .amount(new BigDecimal("200")).paymentMethod("CASH")
                .status("CLEARED").remarks("Misc")
                .allocations(Collections.emptyList()).build();
        when(paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc()).thenReturn(List.of(p1, p2));
        mockSupplierName();

        List<PaymentResponseDto> hits = service.listPayments(null, null, null, null, "077");
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getPaymentId()).isEqualTo(2L);

        List<PaymentResponseDto> remarksHit = service.listPayments(null, null, null, null, "AUG");
        assertThat(remarksHit).hasSize(1);
        assertThat(remarksHit.get(0).getPaymentId()).isEqualTo(1L);
    }

    // -- getPayment -----------------------------------------------------

    @Test
    void getPayment_byIdReturnsDto() {
        SupplierPayment p = SupplierPayment.builder()
                .paymentId(7L).paymentRef("PMT-2026-007")
                .paymentDate(LocalDate.now()).supplierId(SUP)
                .amount(new BigDecimal("123")).paymentMethod("CHEQUE")
                .status("CLEARED").allocations(Collections.emptyList()).build();
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(p));
        mockSupplierName();

        PaymentResponseDto response = service.getPayment(7L);

        assertThat(response.getPaymentId()).isEqualTo(7L);
        assertThat(response.getPaymentMethod()).isEqualTo("Cheque");
        assertThat(response.getSupplierName()).isEqualTo("Fresh Farms Ltd");
    }

    @Test
    void getPayment_unknownIdRejected() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // -- normalisation helpers -----------------------------------------

    @Test
    void normalizeMethod_acceptsDisplayAndCanonical() {
        assertThat(SupplierPaymentServiceImpl.normalizeMethod("Bank Transfer")).isEqualTo("BANK_TRANSFER");
        assertThat(SupplierPaymentServiceImpl.normalizeMethod("BANK_TRANSFER")).isEqualTo("BANK_TRANSFER");
        assertThat(SupplierPaymentServiceImpl.normalizeMethod("cheque")).isEqualTo("CHEQUE");
    }

    @Test
    void toDisplayMethod_canonicalisesToTitleCase() {
        assertThat(SupplierPaymentServiceImpl.toDisplayMethod("BANK_TRANSFER")).isEqualTo("Bank Transfer");
        assertThat(SupplierPaymentServiceImpl.toDisplayMethod("CASH")).isEqualTo("Cash");
        assertThat(SupplierPaymentServiceImpl.toDisplayMethod("CREDIT_CARD")).isEqualTo("Credit Card");
    }

    @Test
    void displayStatus_mapsClearedAndDefaultsPending() {
        assertThat(SupplierPaymentServiceImpl.displayStatus("CLEARED")).isEqualTo("Cleared");
        assertThat(SupplierPaymentServiceImpl.displayStatus("PENDING")).isEqualTo("Pending");
        assertThat(SupplierPaymentServiceImpl.displayStatus(null)).isEqualTo("Pending");
    }
}
