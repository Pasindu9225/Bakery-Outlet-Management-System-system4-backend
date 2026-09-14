package com.plover.backerymanagmentsystem.finance.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.finance.dto.OutstandingGrnDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDetailDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDto;
import com.plover.backerymanagmentsystem.finance.model.ManualLedgerAdjustment;
import com.plover.backerymanagmentsystem.finance.model.PurchaseReturn;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.repository.ManualLedgerAdjustmentRepository;
import com.plover.backerymanagmentsystem.finance.repository.PurchaseReturnRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentAllocationRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.finance.service.OutstandingSummaryService;
import com.plover.backerymanagmentsystem.finance.service.SupplierPaymentService;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link OutstandingSummaryService}.
 *
 * <p>For each supplier we walk their open GRNs (status {@code RECEIVED} or
 * {@code PARTIAL}), compute the per-GRN outstanding balance via the existing
 * {@code SupplierPaymentAllocationRepository.sumAllocatedByGrnId(...)} aggregate,
 * then subtract the supplier-wide sum of {@link PurchaseReturn} totals. The
 * resulting projection mirrors the shape used by the frontend mock data so
 * the wiring is a drop-in replacement.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OutstandingSummaryServiceImpl implements OutstandingSummaryService {

    /** Mirrors {@link SupplierPaymentServiceImpl#OVERDUE_THRESHOLD_DAYS}. */
    static final int DUE_DAYS = 30;

    private final SupplierRepository supplierRepository;
    private final GrnRepository grnRepository;
    private final SupplierPaymentAllocationRepository allocationRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final SupplierPaymentRepository paymentRepository;
    private final ManualLedgerAdjustmentRepository adjustmentRepository;
    private final SupplierPaymentService paymentService;

    @Override
    public List<SupplierOutstandingDto> getSummary(String search, Boolean overdueOnly,
                                                   LocalDate dueFrom, LocalDate dueTo,
                                                   Boolean includeAll) {
        log.info("Finance: outstanding-summary search={} overdueOnly={} dueFrom={} dueTo={} includeAll={}",
                search, overdueOnly, dueFrom, dueTo, includeAll);

        boolean overdueOnlyFlag = Boolean.TRUE.equals(overdueOnly);
        boolean includeAllFlag = Boolean.TRUE.equals(includeAll);
        String needle = (search != null && !search.isBlank())
                ? search.trim().toLowerCase(Locale.ROOT)
                : null;

        LocalDate today = LocalDate.now();
        List<Supplier> suppliers = supplierRepository.findAll();

        List<SupplierOutstandingDto> result = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            SupplierOutstandingDto row = buildRow(supplier, today);

            // Skip zero-outstanding suppliers unless caller explicitly asked
            // for the full list.
            if (!includeAllFlag && row.getTotalOutstanding().signum() <= 0) {
                continue;
            }

            // Apply server-side filters.
            if (overdueOnlyFlag && row.getOverdueAmount().signum() <= 0) {
                continue;
            }
            if (!matchesSearch(row, needle)) {
                continue;
            }
            if (dueFrom != null && (row.getEarliestDueDate() == null
                    || row.getEarliestDueDate().isBefore(dueFrom))) {
                continue;
            }
            if (dueTo != null && (row.getEarliestDueDate() == null
                    || row.getEarliestDueDate().isAfter(dueTo))) {
                continue;
            }

            result.add(row);
        }

        // Default sort: largest outstanding first; ties broken by supplier id
        // for determinism.
        result.sort(Comparator
                .comparing(SupplierOutstandingDto::getTotalOutstanding,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SupplierOutstandingDto::getSupplierId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    public SupplierOutstandingDetailDto getDetail(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("supplierId is required");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        SupplierOutstandingDto row = buildRow(supplier, LocalDate.now());
        // Reuse the FR-FIN-03 projection so the drill-down matches the
        // settle-payments screen exactly.
        List<OutstandingGrnDto> invoices = paymentService.getOutstandingGrns(supplierId);

        return SupplierOutstandingDetailDto.builder()
                .id(row.getId())
                .supplierId(row.getSupplierId())
                .name(row.getName())
                .type(row.getType())
                .totalOutstanding(row.getTotalOutstanding())
                .overdueAmount(row.getOverdueAmount())
                .unpaidInvoices(row.getUnpaidInvoices())
                .earliestDueDate(row.getEarliestDueDate())
                .lastTransaction(row.getLastTransaction())
                .phone(row.getPhone())
                .email(row.getEmail())
                .address(row.getAddress())
                .invoices(invoices)
                .build();
    }

    /**
     * Build the per-supplier rollup row. Returns a zero-outstanding row when
     * the supplier has no open GRNs; the caller decides whether to keep or
     * skip it.
     */
    private SupplierOutstandingDto buildRow(Supplier supplier, LocalDate today) {
        Long supplierId = supplier.getSupplierId();
        List<Grn> grns = grnRepository.findBySupplierId(supplierId);

        BigDecimal grnOutstanding = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        int unpaidInvoices = 0;
        LocalDate earliestDueDate = null;
        LocalDate lastGrnDate = null;

        for (Grn g : grns) {
            if (g.getGrnStatus() != GrnStatus.RECEIVED && g.getGrnStatus() != GrnStatus.PARTIAL) {
                continue;
            }
            BigDecimal total = nz(g.getTotal());
            BigDecimal paid = nz(allocationRepository.sumAllocatedByGrnId(g.getGrnId()));
            BigDecimal outstanding = total.subtract(paid);

            LocalDate received = g.getReceivedDate() != null ? g.getReceivedDate().toLocalDate() : null;
            if (received != null && (lastGrnDate == null || received.isAfter(lastGrnDate))) {
                lastGrnDate = received;
            }

            if (outstanding.signum() <= 0) {
                continue;
            }

            grnOutstanding = grnOutstanding.add(outstanding);
            unpaidInvoices++;

            LocalDate due = received != null ? received.plusDays(DUE_DAYS) : null;
            if (due != null) {
                if (earliestDueDate == null || due.isBefore(earliestDueDate)) {
                    earliestDueDate = due;
                }
                if (today.isAfter(due)) {
                    overdueAmount = overdueAmount.add(outstanding);
                }
            }
        }

        // Returns reduce supplier outstanding (treated as supplier-wide credit
        // per the FR-FIN-02 spec).
        BigDecimal returnsTotal = BigDecimal.ZERO;
        LocalDate lastReturnDate = null;
        for (PurchaseReturn r : purchaseReturnRepository.findBySupplierId(supplierId)) {
            returnsTotal = returnsTotal.add(nz(r.getTotalAmount()));
            if (r.getReturnDate() != null
                    && (lastReturnDate == null || r.getReturnDate().isAfter(lastReturnDate))) {
                lastReturnDate = r.getReturnDate();
            }
        }
        BigDecimal totalAdjustments = BigDecimal.ZERO;
        LocalDate lastAdjDate = null;
        for (ManualLedgerAdjustment a : adjustmentRepository.findBySupplierId(supplierId)) {
            totalAdjustments = totalAdjustments.add(nz(a.getAmount()));
            if (a.getAdjustmentDate() != null
                    && (lastAdjDate == null || a.getAdjustmentDate().isAfter(lastAdjDate))) {
                lastAdjDate = a.getAdjustmentDate();
            }
        }

        // totalOutstanding = (Grns - Paid) - Returns - ManualDebits
        // (Note: manual negative amounts are credits which increase outstanding)
        BigDecimal totalOutstanding = grnOutstanding.subtract(returnsTotal).subtract(totalAdjustments);
        if (totalOutstanding.signum() < 0) {
            totalOutstanding = BigDecimal.ZERO;
        }

        // Latest of the four transaction dates.
        LocalDate lastPaymentDate = null;
        for (SupplierPayment p : paymentRepository.findBySupplierId(supplierId)) {
            if (p.getPaymentDate() != null
                    && (lastPaymentDate == null || p.getPaymentDate().isAfter(lastPaymentDate))) {
                lastPaymentDate = p.getPaymentDate();
            }
        }
        LocalDate lastTransaction = maxDate(maxDate(maxDate(lastGrnDate, lastPaymentDate), lastReturnDate), lastAdjDate);

        return SupplierOutstandingDto.builder()
                .id(String.format("SUP-%03d", supplierId))
                .supplierId(supplierId)
                .name(supplier.getName())
                .type("Supplier")
                .totalOutstanding(totalOutstanding)
                .overdueAmount(overdueAmount)
                .unpaidInvoices(unpaidInvoices)
                .earliestDueDate(earliestDueDate)
                .lastTransaction(lastTransaction)
                .phone(supplier.getContactNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .build();
    }

    /** Match supplier name OR display id (case-insensitive substring). */
    private boolean matchesSearch(SupplierOutstandingDto row, String needle) {
        if (needle == null) {
            return true;
        }
        if (row.getName() != null
                && row.getName().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (row.getId() != null
                && row.getId().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return false;
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
