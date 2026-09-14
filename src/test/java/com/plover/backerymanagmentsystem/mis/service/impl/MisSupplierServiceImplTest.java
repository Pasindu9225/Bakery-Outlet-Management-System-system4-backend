package com.plover.backerymanagmentsystem.mis.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.finance.dto.LedgerEntryDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDto;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.finance.service.OutstandingSummaryService;
import com.plover.backerymanagmentsystem.finance.service.SupplierLedgerService;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierDetailDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierListItemDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierPoGrnDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierSummaryDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierTransactionDto;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

/**
 * Tests for {@link MisSupplierServiceImpl}.
 *
 * <p>Three suppliers are seeded:</p>
 * <ul>
 *   <li>A (id 1) — recent GRN → Active, $300 outstanding (no overdue).</li>
 *   <li>B (id 2) — recent payment but no GRN in 95 days → still Active
 *       (payments count too), $1200 outstanding with $400 overdue.</li>
 *   <li>C (id 3) — no GRN or payment in 200 days → Inactive, $0 outstanding.</li>
 * </ul>
 *
 * <p>Lenient stubbing is used because not every test exercises every
 * supplier; un-stubbed repository calls quietly return empty lists.</p>
 */
@ExtendWith(MockitoExtension.class)
class MisSupplierServiceImplTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private GrnRepository grnRepository;
    @Mock private SupplierPaymentRepository paymentRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private OutstandingSummaryService outstandingSummaryService;
    @Mock private SupplierLedgerService supplierLedgerService;

    @InjectMocks private MisSupplierServiceImpl service;

    private static final Long SUP_A = 1L;
    private static final Long SUP_B = 2L;
    private static final Long SUP_C = 3L;

    // ──────────────────────────────────────────────────────────────────
    // Fixture helpers
    // ──────────────────────────────────────────────────────────────────

    private Supplier supplier(Long id, String name, LocalDateTime createdAt) {
        return Supplier.builder()
                .supplierId(id)
                .name(name)
                .contactNumber("+94 11 000 " + id)
                .email("info@" + name.toLowerCase().replace(" ", "") + ".lk")
                .address(id + " Test Lane, Colombo")
                .repName("Rep " + id)
                .repContactNo("+94 77 000 " + id)
                .bankDetails("Bank " + id)
                .vatStatus("Registered")
                .createdAt(createdAt)
                .build();
    }

    private Grn grn(Long id, Long supplierId, LocalDate date, BigDecimal total, GrnStatus status) {
        return Grn.builder()
                .grnId(id)
                .receivedDate(date != null ? date.atStartOfDay() : null)
                .poId(100L + id)
                .supplierId(supplierId)
                .total(total)
                .grnStatus(status)
                .invoiceNumber("INV-" + id)
                .build();
    }

    private SupplierPayment payment(Long supplierId, LocalDate date, BigDecimal amount) {
        SupplierPayment p = new SupplierPayment();
        p.setPaymentId(supplierId * 1000 + (long) date.getDayOfYear());
        p.setSupplierId(supplierId);
        p.setPaymentDate(date);
        p.setAmount(amount);
        p.setPaymentRef("PMT-2026-" + p.getPaymentId());
        p.setStatus("CLEARED");
        p.setPaymentMethod("CASH");
        return p;
    }

    private SupplierOutstandingDto rollup(Long supplierId, BigDecimal outstanding,
                                          BigDecimal overdue, LocalDate lastTx) {
        return SupplierOutstandingDto.builder()
                .id(String.format("SUP-%03d", supplierId))
                .supplierId(supplierId)
                .name("Supplier " + supplierId)
                .type("Supplier")
                .totalOutstanding(outstanding)
                .overdueAmount(overdue)
                .unpaidInvoices(outstanding.signum() > 0 ? 1 : 0)
                .lastTransaction(lastTx)
                .build();
    }

    /** Seed the canonical three-supplier fixture. */
    private void seed() {
        Supplier a = supplier(SUP_A, "Alpha Foods", LocalDateTime.of(2024, 3, 15, 0, 0));
        Supplier b = supplier(SUP_B, "Beta Bakers", LocalDateTime.of(2023, 7, 1, 0, 0));
        Supplier c = supplier(SUP_C, "Cocoa Co", LocalDateTime.of(2022, 11, 8, 0, 0));
        lenient().when(supplierRepository.findAll()).thenReturn(List.of(a, b, c));
        lenient().when(supplierRepository.findById(SUP_A)).thenReturn(Optional.of(a));
        lenient().when(supplierRepository.findById(SUP_B)).thenReturn(Optional.of(b));
        lenient().when(supplierRepository.findById(SUP_C)).thenReturn(Optional.of(c));

        // Supplier A: a GRN 5 days ago → active.
        Grn a1 = grn(11L, SUP_A, LocalDate.now().minusDays(5),
                new BigDecimal("300.00"), GrnStatus.RECEIVED);
        lenient().when(grnRepository.findBySupplierId(SUP_A)).thenReturn(List.of(a1));

        // Supplier B: GRN 95 days ago (outside the active window) but a payment
        // 30 days ago → still active because payments count.
        Grn b1 = grn(21L, SUP_B, LocalDate.now().minusDays(95),
                new BigDecimal("1200.00"), GrnStatus.RECEIVED);
        lenient().when(grnRepository.findBySupplierId(SUP_B)).thenReturn(List.of(b1));
        lenient().when(paymentRepository.findBySupplierId(SUP_B))
                .thenReturn(List.of(payment(SUP_B, LocalDate.now().minusDays(30),
                        new BigDecimal("100.00"))));

        // Supplier C: nothing recent → inactive, no outstanding.
        Grn c1 = grn(31L, SUP_C, LocalDate.now().minusDays(200),
                new BigDecimal("400.00"), GrnStatus.RECEIVED);
        lenient().when(grnRepository.findBySupplierId(SUP_C)).thenReturn(List.of(c1));

        // Default empty payments / POs for A and C.
        lenient().when(paymentRepository.findBySupplierId(SUP_A)).thenReturn(Collections.emptyList());
        lenient().when(paymentRepository.findBySupplierId(SUP_C)).thenReturn(Collections.emptyList());
        lenient().when(purchaseOrderRepository.findBySupplierId(any())).thenReturn(Collections.emptyList());

        // Outstanding summary rollups (the finance service is mocked; we
        // hand-craft the response so we can assert how the MIS service
        // combines it with master data).
        lenient().when(outstandingSummaryService.getSummary(any(), any(), any(), any(), eq(true)))
                .thenReturn(List.of(
                        rollup(SUP_A, new BigDecimal("300.00"), BigDecimal.ZERO,
                                LocalDate.now().minusDays(5)),
                        rollup(SUP_B, new BigDecimal("1200.00"), new BigDecimal("400.00"),
                                LocalDate.now().minusDays(30)),
                        rollup(SUP_C, BigDecimal.ZERO, BigDecimal.ZERO, null)
                ));
    }

    // ──────────────────────────────────────────────────────────────────
    // Status derivation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void deriveStatus_isActiveWhenRecentGrnExists() {
        seed();
        assertThat(service.deriveStatus(SUP_A, LocalDate.now())).isEqualTo("Active");
    }

    @Test
    void deriveStatus_isActiveWhenRecentPaymentExists() {
        seed();
        // Supplier B's latest GRN is 95 days old (outside the 90-day window),
        // but a payment 30 days ago keeps them active.
        assertThat(service.deriveStatus(SUP_B, LocalDate.now())).isEqualTo("Active");
    }

    @Test
    void deriveStatus_isInactiveWhenNoRecentActivity() {
        seed();
        assertThat(service.deriveStatus(SUP_C, LocalDate.now())).isEqualTo("Inactive");
    }

    // ──────────────────────────────────────────────────────────────────
    // Registration id formatting
    // ──────────────────────────────────────────────────────────────────

    @Test
    void deriveRegistrationId_formatsYearAndZeroPaddedId() {
        assertThat(MisSupplierServiceImpl.deriveRegistrationId(42L, LocalDate.of(2024, 3, 15)))
                .isEqualTo("REG-2024-0042");
        assertThat(MisSupplierServiceImpl.deriveRegistrationId(7L, LocalDate.of(2020, 1, 1)))
                .isEqualTo("REG-2020-0007");
        // Falls back to current year when createdAt is null.
        assertThat(MisSupplierServiceImpl.deriveRegistrationId(1L, null))
                .startsWith("REG-").endsWith("-0001");
    }

    // ──────────────────────────────────────────────────────────────────
    // Summary aggregation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getSummary_aggregatesAcrossAllSuppliers() {
        seed();
        MisSupplierSummaryDto sum = service.getSummary();

        assertThat(sum.getTotalSuppliers()).isEqualTo(3);
        // A and B are active; C is not.
        assertThat(sum.getActiveCount()).isEqualTo(2);
        // 300 + 1200 + 0
        assertThat(sum.getTotalOutstanding()).isEqualByComparingTo("1500.00");
        // Only B has overdueAmount > 0.
        assertThat(sum.getOverdueCount()).isEqualTo(1);
    }

    // ──────────────────────────────────────────────────────────────────
    // List filters & validation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getSuppliers_returnsAllRowsWhenNoFiltersApplied() {
        seed();
        List<MisSupplierListItemDto> rows = service.getSuppliers(null, null, null, null);
        assertThat(rows).extracting(MisSupplierListItemDto::getSupplierId)
                .containsExactlyInAnyOrder(SUP_A, SUP_B, SUP_C);
        // Verify the status field landed for each row.
        assertThat(rows.stream()
                .filter(r -> r.getSupplierId().equals(SUP_A)).findFirst().orElseThrow()
                .getStatus()).isEqualTo("Active");
        assertThat(rows.stream()
                .filter(r -> r.getSupplierId().equals(SUP_C)).findFirst().orElseThrow()
                .getStatus()).isEqualTo("Inactive");
    }

    @Test
    void getSuppliers_statusFilterRetainsActiveOnly() {
        seed();
        List<MisSupplierListItemDto> rows = service.getSuppliers(null, "Active", null, null);
        assertThat(rows).extracting(MisSupplierListItemDto::getSupplierId)
                .containsExactlyInAnyOrder(SUP_A, SUP_B);
    }

    @Test
    void getSuppliers_searchMatchesName() {
        seed();
        List<MisSupplierListItemDto> rows = service.getSuppliers("alpha", null, null, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSupplierId()).isEqualTo(SUP_A);
    }

    @Test
    void getSuppliers_searchMatchesDisplayId() {
        seed();
        List<MisSupplierListItemDto> rows = service.getSuppliers("SUP-003", null, null, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSupplierId()).isEqualTo(SUP_C);
    }

    @Test
    void getSuppliers_dateFilterRestrictsByLastTransaction() {
        seed();
        // Window covers only A (5 days ago) — exclude B (30 days) and C (null).
        List<MisSupplierListItemDto> rows = service.getSuppliers(null, null,
                LocalDate.now().minusDays(10), LocalDate.now());
        assertThat(rows).extracting(MisSupplierListItemDto::getSupplierId)
                .containsExactly(SUP_A);
    }

    @Test
    void getSuppliers_rejectsInvalidStatus() {
        seed();
        assertThatThrownBy(() -> service.getSuppliers(null, "Maybe", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getSuppliers_rejectsInvertedDateRange() {
        seed();
        assertThatThrownBy(() -> service.getSuppliers(null, null,
                LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ──────────────────────────────────────────────────────────────────
    // Detail / 404 path
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getSupplierDetail_throws404WhenMissing() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSupplierDetail(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getSupplierDetail_populatesProfileAndCounts() {
        seed();
        MisSupplierDetailDto d = service.getSupplierDetail(SUP_A);
        assertThat(d.getId()).isEqualTo("SUP-001");
        assertThat(d.getName()).isEqualTo("Alpha Foods");
        assertThat(d.getRepName()).isEqualTo("Rep 1");
        assertThat(d.getBankDetails()).isEqualTo("Bank 1");
        assertThat(d.getVatStatus()).isEqualTo("Registered");
        assertThat(d.getStatus()).isEqualTo("Active");
        assertThat(d.getOutstandingBalance()).isEqualByComparingTo("300.00");
        assertThat(d.isOverdue()).isFalse();
        assertThat(d.getRegistrationId()).isEqualTo("REG-2024-0001");
        assertThat(d.getRegistrationDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        // 1 GRN, 0 POs (no PO repo stubs returned data above).
        assertThat(d.getTotalGrns()).isEqualTo(1L);
        assertThat(d.getTotalPos()).isEqualTo(0L);
    }

    // ──────────────────────────────────────────────────────────────────
    // Transactions
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getSupplierTransactions_searchFiltersByRefAndDescription() {
        seed();
        when(supplierLedgerService.getLedger(eq(SUP_A), any(), any(), any(), any()))
                .thenReturn(List.of(
                        LedgerEntryDto.builder().id("L-GRN-1").ref("GRN-1")
                                .description("Goods Received").type("GRN")
                                .debit(new BigDecimal("100")).credit(BigDecimal.ZERO)
                                .balance(new BigDecimal("100")).status("Pending").build(),
                        LedgerEntryDto.builder().id("L-PMT-1").ref("PMT-2026-001")
                                .description("Payment - CASH").type("Payment")
                                .debit(BigDecimal.ZERO).credit(new BigDecimal("50"))
                                .balance(new BigDecimal("50")).status("Cleared").build()));

        List<MisSupplierTransactionDto> rows = service.getSupplierTransactions(SUP_A, "payment");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getRef()).isEqualTo("PMT-2026-001");
        assertThat(rows.get(0).getType()).isEqualTo("Payment");

        // Empty search returns everything.
        List<MisSupplierTransactionDto> all = service.getSupplierTransactions(SUP_A, null);
        assertThat(all).hasSize(2);
    }

    // ──────────────────────────────────────────────────────────────────
    // PO / GRN listing
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getSupplierPoGrn_marksDeliveredAndPending() {
        seed();
        // Wire two POs for supplier A: one delivered, one without a GRN.
        RawMaterial chicken = RawMaterial.builder().id(1L).materialName("Chicken").unitOfMeasure("kg").build();
        RawMaterial herbs = RawMaterial.builder().id(2L).materialName("Herbs").unitOfMeasure("kg").build();

        PurchaseOrderItem chickenItem = PurchaseOrderItem.builder()
                .poiId(1L).rawMaterialId(1).requiredQty(120)
                .estimatedCost(new BigDecimal("4800.00")).unitOfMeasure("kg")
                .rawMaterial(chicken).build();
        PurchaseOrderItem extraItem = PurchaseOrderItem.builder()
                .poiId(2L).rawMaterialId(2).requiredQty(20)
                .estimatedCost(new BigDecimal("700.00")).unitOfMeasure("kg")
                .rawMaterial(herbs).build();

        PurchaseOrder delivered = PurchaseOrder.builder()
                .poId(74L).supplierId(SUP_A)
                .totalCost(new BigDecimal("4800.00")).numberOfItems(120)
                .estimatedDeliveryDate(LocalDate.of(2025, 8, 20))
                .status("Approved")
                .purchaseOrderItems(List.of(chickenItem, extraItem))
                .build();
        PurchaseOrder pending = PurchaseOrder.builder()
                .poId(44L).supplierId(SUP_A)
                .totalCost(new BigDecimal("700.00")).numberOfItems(10)
                .estimatedDeliveryDate(LocalDate.of(2025, 7, 18))
                .status("Approved")
                .purchaseOrderItems(List.of(extraItem))
                .build();
        when(purchaseOrderRepository.findBySupplierId(SUP_A))
                .thenReturn(List.of(delivered, pending));

        // Delivered PO has a corresponding GRN.
        Grn deliveredGrn = grn(88L, SUP_A, LocalDate.of(2025, 8, 28),
                new BigDecimal("4800.00"), GrnStatus.RECEIVED);
        deliveredGrn.setPoId(74L);
        deliveredGrn.setGrnItems(List.of(GrnItem.builder()
                .grnItemId(880L).grnId(88L).rawMaterialId(1L).uom("kg")
                .receivedQuantity(new BigDecimal("120"))
                .pricePerUnit(new BigDecimal("40")).build()));
        when(grnRepository.findBySupplierId(SUP_A)).thenReturn(List.of(deliveredGrn));

        List<MisSupplierPoGrnDto> rows = service.getSupplierPoGrn(SUP_A);

        assertThat(rows).hasSize(2);
        // Delivered PO date 2025-08-20 sorts ahead of pending 2025-07-18.
        MisSupplierPoGrnDto deliveredRow = rows.get(0);
        assertThat(deliveredRow.getPoNo()).isEqualTo("PO-74");
        assertThat(deliveredRow.getGrnNo()).isEqualTo("GRN-88");
        assertThat(deliveredRow.getStatus()).isEqualTo("Delivered");
        assertThat(deliveredRow.getProductSummary()).isEqualTo("Chicken +1 more");
        assertThat(deliveredRow.getQty()).isEqualByComparingTo("120");
        assertThat(deliveredRow.getAmount()).isEqualByComparingTo("4800.00");

        MisSupplierPoGrnDto pendingRow = rows.get(1);
        assertThat(pendingRow.getPoNo()).isEqualTo("PO-44");
        assertThat(pendingRow.getGrnNo()).isEqualTo("—");
        assertThat(pendingRow.getGrnDate()).isNull();
        assertThat(pendingRow.getStatus()).isEqualTo("Pending");
        assertThat(pendingRow.getProductSummary()).isEqualTo("Herbs");
        assertThat(pendingRow.getAmount()).isEqualByComparingTo("700.00");
    }

    @Test
    void getSupplierPoGrn_throws404WhenSupplierMissing() {
        when(supplierRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSupplierPoGrn(404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
