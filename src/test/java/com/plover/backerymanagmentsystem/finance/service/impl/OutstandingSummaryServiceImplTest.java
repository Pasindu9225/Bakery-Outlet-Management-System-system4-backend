package com.plover.backerymanagmentsystem.finance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.finance.dto.OutstandingGrnDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDetailDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDto;
import com.plover.backerymanagmentsystem.finance.repository.ManualLedgerAdjustmentRepository;
import com.plover.backerymanagmentsystem.finance.repository.PurchaseReturnRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentAllocationRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.finance.service.SupplierPaymentService;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

/**
 * Tests for {@link OutstandingSummaryServiceImpl}. Three suppliers are seeded:
 * <ul>
 *   <li>A: 1 GRN $1000 fully paid → excluded by default.</li>
 *   <li>B: 1 GRN $500 with $200 paid → outstanding $300.</li>
 *   <li>C: 2 GRNs ($400 60d ago overdue, $600 5d ago) no payments → outstanding $1000, overdue $400.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OutstandingSummaryServiceImplTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private GrnRepository grnRepository;
    @Mock private SupplierPaymentAllocationRepository allocationRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;
    @Mock private SupplierPaymentRepository paymentRepository;
    @Mock private SupplierPaymentService paymentService;
    @Mock private ManualLedgerAdjustmentRepository adjustmentRepository;

    @InjectMocks private OutstandingSummaryServiceImpl service;

    private static final Long SUP_A = 1L;
    private static final Long SUP_B = 2L;
    private static final Long SUP_C = 3L;

    // GRN ids
    private static final Long GRN_A1 = 11L;
    private static final Long GRN_B1 = 21L;
    private static final Long GRN_C1 = 31L; // 60 days old → overdue
    private static final Long GRN_C2 = 32L; // 5 days old → not overdue

    private Supplier supplier(Long id, String name) {
        return Supplier.builder()
                .supplierId(id)
                .name(name)
                .contactNumber("+94 11 000 " + id)
                .email("info@" + name.toLowerCase().replace(" ", "") + ".lk")
                .address(id + " Test Lane, Colombo")
                .build();
    }

    private Grn grn(Long id, Long supplierId, LocalDate date, BigDecimal total) {
        return Grn.builder()
                .grnId(id)
                .receivedDate(date.atStartOfDay())
                .poId(100L + id)
                .supplierId(supplierId)
                .total(total)
                .grnStatus(GrnStatus.RECEIVED)
                .invoiceNumber("INV-" + id)
                .build();
    }

    /** Wire up the three-supplier fixture. Uses lenient stubs because not
     *  every test exercises every supplier's helper repositories. */
    private void seed() {
        Supplier a = supplier(SUP_A, "Alpha Foods");
        Supplier b = supplier(SUP_B, "Beta Bakers");
        Supplier c = supplier(SUP_C, "Cocoa Co");
        lenient().when(supplierRepository.findAll()).thenReturn(List.of(a, b, c));

        // Supplier A: one GRN fully paid.
        Grn a1 = grn(GRN_A1, SUP_A, LocalDate.now().minusDays(10), new BigDecimal("1000.00"));
        lenient().when(grnRepository.findBySupplierId(SUP_A)).thenReturn(List.of(a1));
        lenient().when(allocationRepository.sumAllocatedByGrnId(GRN_A1))
                .thenReturn(new BigDecimal("1000.00"));

        // Supplier B: one GRN partially paid ($500 - $200 = $300 outstanding).
        Grn b1 = grn(GRN_B1, SUP_B, LocalDate.now().minusDays(5), new BigDecimal("500.00"));
        lenient().when(grnRepository.findBySupplierId(SUP_B)).thenReturn(List.of(b1));
        lenient().when(allocationRepository.sumAllocatedByGrnId(GRN_B1))
                .thenReturn(new BigDecimal("200.00"));

        // Supplier C: two GRNs no payments.
        // GRN C1: $400, received 60 days ago → due 30d ago → overdue.
        // GRN C2: $600, received 5 days ago → due in 25 days → not overdue.
        Grn c1 = grn(GRN_C1, SUP_C, LocalDate.now().minusDays(60), new BigDecimal("400.00"));
        Grn c2 = grn(GRN_C2, SUP_C, LocalDate.now().minusDays(5), new BigDecimal("600.00"));
        lenient().when(grnRepository.findBySupplierId(SUP_C)).thenReturn(List.of(c1, c2));
        lenient().when(allocationRepository.sumAllocatedByGrnId(GRN_C1)).thenReturn(BigDecimal.ZERO);
        lenient().when(allocationRepository.sumAllocatedByGrnId(GRN_C2)).thenReturn(BigDecimal.ZERO);

        // No returns or payments by default.
        lenient().when(purchaseReturnRepository.findBySupplierId(SUP_A)).thenReturn(Collections.emptyList());
        lenient().when(purchaseReturnRepository.findBySupplierId(SUP_B)).thenReturn(Collections.emptyList());
        lenient().when(purchaseReturnRepository.findBySupplierId(SUP_C)).thenReturn(Collections.emptyList());
        lenient().when(paymentRepository.findBySupplierId(SUP_A)).thenReturn(Collections.emptyList());
        lenient().when(paymentRepository.findBySupplierId(SUP_B)).thenReturn(Collections.emptyList());
        lenient().when(paymentRepository.findBySupplierId(SUP_C)).thenReturn(Collections.emptyList());
        lenient().when(adjustmentRepository.findBySupplierId(SUP_A)).thenReturn(Collections.emptyList());
        lenient().when(adjustmentRepository.findBySupplierId(SUP_B)).thenReturn(Collections.emptyList());
        lenient().when(adjustmentRepository.findBySupplierId(SUP_C)).thenReturn(Collections.emptyList());
    }

    // -- getSummary -----------------------------------------------------

    @Test
    void getSummary_excludesFullyPaidSupplierByDefault() {
        seed();

        List<SupplierOutstandingDto> result = service.getSummary(null, false, null, null, false);

        // Supplier A is fully paid → excluded; B and C remain.
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SupplierOutstandingDto::getSupplierId)
                .containsExactly(SUP_C, SUP_B); // sorted by total DESC: C $1000, B $300
    }

    @Test
    void getSummary_includeAllRetainsZeroSuppliers() {
        seed();

        List<SupplierOutstandingDto> result = service.getSummary(null, false, null, null, true);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(SupplierOutstandingDto::getSupplierId)
                .containsExactly(SUP_C, SUP_B, SUP_A); // C $1000, B $300, A $0
    }

    @Test
    void getSummary_supplierBHasCorrectAggregates() {
        seed();

        SupplierOutstandingDto b = service.getSummary(null, false, null, null, false).stream()
                .filter(r -> SUP_B.equals(r.getSupplierId()))
                .findFirst().orElseThrow();

        assertThat(b.getId()).isEqualTo("SUP-002");
        assertThat(b.getName()).isEqualTo("Beta Bakers");
        assertThat(b.getType()).isEqualTo("Supplier");
        assertThat(b.getTotalOutstanding()).isEqualByComparingTo("300.00");
        assertThat(b.getOverdueAmount()).isEqualByComparingTo("0");
        assertThat(b.getUnpaidInvoices()).isEqualTo(1);
        // GRN B1 received 5d ago, due 25 days from now.
        assertThat(b.getEarliestDueDate()).isEqualTo(LocalDate.now().minusDays(5).plusDays(30));
        assertThat(b.getLastTransaction()).isEqualTo(LocalDate.now().minusDays(5));
        assertThat(b.getPhone()).isEqualTo("+94 11 000 2");
    }

    @Test
    void getSummary_supplierCAggregatesOverdueCorrectly() {
        seed();

        SupplierOutstandingDto c = service.getSummary(null, false, null, null, false).stream()
                .filter(r -> SUP_C.equals(r.getSupplierId()))
                .findFirst().orElseThrow();

        assertThat(c.getTotalOutstanding()).isEqualByComparingTo("1000.00");
        // GRN C1 ($400, received 60d ago) is overdue; C2 ($600) is not.
        assertThat(c.getOverdueAmount()).isEqualByComparingTo("400.00");
        assertThat(c.getUnpaidInvoices()).isEqualTo(2);
        // Earliest due date is the older GRN's receivedDate + 30.
        assertThat(c.getEarliestDueDate()).isEqualTo(LocalDate.now().minusDays(60).plusDays(30));
        // Last transaction is the more recent GRN date (5 days ago).
        assertThat(c.getLastTransaction()).isEqualTo(LocalDate.now().minusDays(5));
    }

    @Test
    void getSummary_searchByNameMatchesOnlyB() {
        seed();

        List<SupplierOutstandingDto> result = service.getSummary("beta", false, null, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierId()).isEqualTo(SUP_B);
    }

    @Test
    void getSummary_searchByDisplayIdMatches() {
        seed();

        List<SupplierOutstandingDto> result = service.getSummary("SUP-003", false, null, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierId()).isEqualTo(SUP_C);
    }

    @Test
    void getSummary_overdueOnlyReturnsCnotB() {
        seed();

        List<SupplierOutstandingDto> result = service.getSummary(null, true, null, null, false);

        // Only C has overdueAmount > 0.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierId()).isEqualTo(SUP_C);
    }

    @Test
    void getSummary_dueDateRangeFiltersByEarliestDue() {
        seed();

        // C's earliest due is today-30 days; B's earliest due is today+25 days.
        // Range [today-50, today-10] should return only C.
        List<SupplierOutstandingDto> result = service.getSummary(
                null, false,
                LocalDate.now().minusDays(50),
                LocalDate.now().minusDays(10),
                false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierId()).isEqualTo(SUP_C);
    }

    @Test
    void getSummary_returnsReduceTotalOutstanding() {
        seed();
        // Add a $100 return for supplier B → outstanding 300 - 100 = 200.
        com.plover.backerymanagmentsystem.finance.model.PurchaseReturn ret =
                com.plover.backerymanagmentsystem.finance.model.PurchaseReturn.builder()
                        .returnId(1L).returnRef("RTN-2026-001")
                        .returnDate(LocalDate.now().minusDays(2))
                        .supplierId(SUP_B).grnId(GRN_B1)
                        .totalAmount(new BigDecimal("100.00"))
                        .build();
        when(purchaseReturnRepository.findBySupplierId(SUP_B)).thenReturn(List.of(ret));

        SupplierOutstandingDto b = service.getSummary(null, false, null, null, false).stream()
                .filter(r -> SUP_B.equals(r.getSupplierId()))
                .findFirst().orElseThrow();

        assertThat(b.getTotalOutstanding()).isEqualByComparingTo("200.00");
        assertThat(b.getLastTransaction()).isEqualTo(LocalDate.now().minusDays(2));
    }

    @Test
    void getSummary_skipsCancelledAndPendingGrns() {
        Supplier d = supplier(4L, "Delta Dairy");
        when(supplierRepository.findAll()).thenReturn(List.of(d));
        Grn pending = Grn.builder()
                .grnId(40L).receivedDate(LocalDate.now().minusDays(1).atStartOfDay())
                .poId(140L).supplierId(4L).total(new BigDecimal("999"))
                .grnStatus(GrnStatus.PENDING).build();
        Grn cancelled = Grn.builder()
                .grnId(41L).receivedDate(LocalDate.now().minusDays(1).atStartOfDay())
                .poId(141L).supplierId(4L).total(new BigDecimal("888"))
                .grnStatus(GrnStatus.CANCELLED).build();
        when(grnRepository.findBySupplierId(4L)).thenReturn(List.of(pending, cancelled));

        List<SupplierOutstandingDto> result = service.getSummary(null, false, null, null, false);

        assertThat(result).isEmpty();
    }

    // -- getDetail ------------------------------------------------------

    @Test
    void getDetail_returnsRollupAndInvoiceList() {
        seed();
        OutstandingGrnDto invoice = OutstandingGrnDto.builder()
                .grnId(GRN_B1)
                .ref("GRN-21")
                .description("Goods Received - INV-21")
                .date(LocalDate.now().minusDays(5).toString())
                .dueDate(LocalDate.now().plusDays(25).toString())
                .amount(new BigDecimal("500.00"))
                .paid(new BigDecimal("200.00"))
                .outstanding(new BigDecimal("300.00"))
                .status("Pending")
                .build();
        when(supplierRepository.findById(SUP_B))
                .thenReturn(Optional.of(supplier(SUP_B, "Beta Bakers")));
        when(paymentService.getOutstandingGrns(eq(SUP_B))).thenReturn(List.of(invoice));

        SupplierOutstandingDetailDto detail = service.getDetail(SUP_B);

        assertThat(detail.getSupplierId()).isEqualTo(SUP_B);
        assertThat(detail.getId()).isEqualTo("SUP-002");
        assertThat(detail.getName()).isEqualTo("Beta Bakers");
        assertThat(detail.getTotalOutstanding()).isEqualByComparingTo("300.00");
        assertThat(detail.getInvoices()).hasSize(1);
        assertThat(detail.getInvoices().get(0).getRef()).isEqualTo("GRN-21");
        assertThat(detail.getInvoices().get(0).getOutstanding()).isEqualByComparingTo("300.00");
    }

    @Test
    void getDetail_unknownSupplierThrows() {
        when(supplierRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getDetail_nullSupplierIdRejected() {
        assertThatThrownBy(() -> service.getDetail(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supplierId");
    }
}
