package com.plover.backerymanagmentsystem.finance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.finance.dto.LedgerEntryDto;
import com.plover.backerymanagmentsystem.finance.model.ManualLedgerAdjustment;
import com.plover.backerymanagmentsystem.finance.model.PurchaseReturn;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.repository.ManualLedgerAdjustmentRepository;
import com.plover.backerymanagmentsystem.finance.repository.PurchaseReturnRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentAllocationRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;

@ExtendWith(MockitoExtension.class)
class SupplierLedgerServiceImplTest {

    @Mock private GrnRepository grnRepository;
    @Mock private SupplierPaymentRepository paymentRepository;
    @Mock private SupplierPaymentAllocationRepository allocationRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;
    @Mock private ManualLedgerAdjustmentRepository adjustmentRepository;

    @InjectMocks private SupplierLedgerServiceImpl service;

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

    private SupplierPayment payment(Long id, LocalDate date, BigDecimal amount, String status) {
        return SupplierPayment.builder()
                .paymentId(id)
                .paymentRef("PMT-2025-" + String.format("%03d", id))
                .paymentDate(date)
                .supplierId(SUP)
                .amount(amount)
                .paymentMethod("BANK_TRANSFER")
                .status(status)
                .build();
    }

    private PurchaseReturn purchaseReturn(Long id, LocalDate date, BigDecimal amount) {
        return PurchaseReturn.builder()
                .returnId(id)
                .returnRef("RTN-2025-" + String.format("%03d", id))
                .returnDate(date)
                .supplierId(SUP)
                .grnId(1L)
                .totalAmount(amount)
                .reason("Damaged")
                .build();
    }

    private void mockEmptyExtras() {
        when(allocationRepository.sumAllocatedByGrnId(any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void getLedger_combinesGrnPaymentAndReturnAndComputesBalance() {
        // 1 GRN ($1000 debit), 1 Payment ($400 credit), 1 Return ($100 credit).
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                grn(1L, LocalDate.now().minusDays(5), new BigDecimal("1000.00"), GrnStatus.RECEIVED)));
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(List.of(
                payment(1L, LocalDate.now().minusDays(2), new BigDecimal("400.00"), "CLEARED")));
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                purchaseReturn(1L, LocalDate.now().minusDays(1), new BigDecimal("100.00"))));
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        mockEmptyExtras();

        List<LedgerEntryDto> entries = service.getLedger(SUP, null, null, null, null);

        assertThat(entries).hasSize(3);
        // Display order is newest first.
        assertThat(entries.get(0).getType()).isEqualTo("Return");
        assertThat(entries.get(1).getType()).isEqualTo("Payment");
        assertThat(entries.get(2).getType()).isEqualTo("GRN");

        // Closing balance after all 3 = 1000 - 400 - 100 = 500.
        assertThat(entries.get(0).getBalance()).isEqualByComparingTo("500");

        BigDecimal totalDebit = entries.stream().map(LedgerEntryDto::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entries.stream().map(LedgerEntryDto::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo("1000");
        assertThat(totalCredit).isEqualByComparingTo("500");
    }

    @Test
    void getLedger_dateRangeFilter() {
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                grn(1L, LocalDate.of(2025, 1, 1), new BigDecimal("100"), GrnStatus.RECEIVED),
                grn(2L, LocalDate.of(2025, 6, 15), new BigDecimal("200"), GrnStatus.RECEIVED)));
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        mockEmptyExtras();

        List<LedgerEntryDto> entries = service.getLedger(
                SUP, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), null, null);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getRef()).isEqualTo("GRN-2");
    }

    @Test
    void getLedger_typeFilterPickPaymentsOnly() {
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                grn(1L, LocalDate.now().minusDays(5), new BigDecimal("100"), GrnStatus.RECEIVED)));
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(List.of(
                payment(1L, LocalDate.now().minusDays(2), new BigDecimal("50"), "CLEARED")));
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        mockEmptyExtras();

        List<LedgerEntryDto> entries = service.getLedger(SUP, null, null, "Payment", null);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getType()).isEqualTo("Payment");
    }

    @Test
    void getLedger_grnOverdueWhenOlderThan30Days() {
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                grn(1L, LocalDate.now().minusDays(45), new BigDecimal("100"), GrnStatus.RECEIVED)));
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        mockEmptyExtras();

        List<LedgerEntryDto> entries = service.getLedger(SUP, null, null, null, null);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getStatus()).isEqualTo("Overdue");
    }

    @Test
    void getLedger_grnClearedWhenFullyAllocated() {
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                grn(1L, LocalDate.now().minusDays(5), new BigDecimal("500"), GrnStatus.RECEIVED)));
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(allocationRepository.sumAllocatedByGrnId(1L)).thenReturn(new BigDecimal("500"));

        List<LedgerEntryDto> entries = service.getLedger(SUP, null, null, null, null);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getStatus()).isEqualTo("Cleared");
    }

    @Test
    void getLedger_pendingGrnIsExcluded() {
        when(grnRepository.findBySupplierId(SUP)).thenReturn(List.of(
                grn(1L, LocalDate.now().minusDays(5), new BigDecimal("100"), GrnStatus.PENDING)));
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());

        List<LedgerEntryDto> entries = service.getLedger(SUP, null, null, null, null);

        assertThat(entries).isEmpty();
    }

    @Test
    void getLedger_negativeAdjustmentIsCredit() {
        ManualLedgerAdjustment adj = ManualLedgerAdjustment.builder()
                .adjustmentId(1L)
                .adjustmentRef("ADJ-2025-001")
                .adjustmentDate(LocalDate.now().minusDays(2))
                .supplierId(SUP)
                .amount(new BigDecimal("-50.00"))
                .description("Discount")
                .remarks("Approved discount")
                .createdAt(LocalDateTime.now())
                .build();
        when(grnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(paymentRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(purchaseReturnRepository.findBySupplierId(SUP)).thenReturn(Collections.emptyList());
        when(adjustmentRepository.findBySupplierId(SUP)).thenReturn(List.of(adj));

        List<LedgerEntryDto> entries = service.getLedger(SUP, null, null, null, null);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getCredit()).isEqualByComparingTo("50.00");
        assertThat(entries.get(0).getDebit()).isEqualByComparingTo("0");
    }

    @Test
    void getLedger_nullSupplierIdRejected() {
        assertThatThrownBy(() -> service.getLedger(null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supplierId");
    }
}
