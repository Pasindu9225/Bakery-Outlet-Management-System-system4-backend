package com.plover.backerymanagmentsystem.mis.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.plover.backerymanagmentsystem.mis.service.MisSupplierService;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link MisSupplierService}.
 *
 * <h3>Status derivation</h3>
 * <p>The {@code Supplier} entity has no {@code status} column. We derive
 * {@code "Active"} when the supplier has at least one GRN
 * ({@code receivedDate}) or supplier payment within the last
 * {@value #ACTIVE_THRESHOLD_DAYS} days; otherwise {@code "Inactive"}. This
 * mirrors the FR-FIN-02 spec's "last transaction" semantics and avoids a
 * schema change.</p>
 *
 * <h3>Synthetic registration id</h3>
 * <p>The schema also lacks a registration-id column, so we synthesise
 * {@code "REG-{year-of-createdAt}-{4-digit-supplierId}"} (e.g.
 * {@code "REG-2024-0042"}). When {@code createdAt} is {@code null} we fall
 * back to {@link LocalDate#now()}.</p>
 *
 * <h3>PO date proxy</h3>
 * <p>{@link PurchaseOrder} has no {@code createdAt} field. The PO/GRN
 * listing reports {@code estimatedDeliveryDate} as the PO date — when a
 * real created-at column is added later this should switch to it.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MisSupplierServiceImpl implements MisSupplierService {

    /** Activity window for the {@code Active} / {@code Inactive} derivation. */
    static final int ACTIVE_THRESHOLD_DAYS = 90;

    static final String STATUS_ACTIVE = "Active";
    static final String STATUS_INACTIVE = "Inactive";
    static final String STATUS_ALL = "All";

    private final SupplierRepository supplierRepository;
    private final GrnRepository grnRepository;
    private final SupplierPaymentRepository paymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OutstandingSummaryService outstandingSummaryService;
    private final SupplierLedgerService supplierLedgerService;

    @Override
    public MisSupplierSummaryDto getSummary() {
        log.info("MIS: GET /suppliers/summary");
        // Use the existing finance rollup as the source of truth for
        // outstanding/overdue figures so we don't duplicate the math.
        List<SupplierOutstandingDto> rollups = outstandingSummaryService
                .getSummary(null, false, null, null, true);
        Map<Long, SupplierOutstandingDto> rollupBySupplier = indexById(rollups);

        List<Supplier> suppliers = supplierRepository.findAll();
        long total = suppliers.size();
        long active = 0;
        BigDecimal outstanding = BigDecimal.ZERO;
        long overdue = 0;
        LocalDate today = LocalDate.now();

        for (Supplier s : suppliers) {
            if (isActive(s.getSupplierId(), today)) {
                active++;
            }
            SupplierOutstandingDto r = rollupBySupplier.get(s.getSupplierId());
            if (r != null) {
                outstanding = outstanding.add(nz(r.getTotalOutstanding()));
                if (nz(r.getOverdueAmount()).signum() > 0) {
                    overdue++;
                }
            }
        }

        return MisSupplierSummaryDto.builder()
                .totalSuppliers(total)
                .activeCount(active)
                .totalOutstanding(outstanding)
                .overdueCount(overdue)
                .build();
    }

    @Override
    public List<MisSupplierListItemDto> getSuppliers(String search, String status,
                                                    LocalDate startDate, LocalDate endDate) {
        log.info("MIS: GET /suppliers search={} status={} startDate={} endDate={}",
                search, status, startDate, endDate);

        validateStatus(status);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate");
        }

        String wantedStatus = normaliseStatusFilter(status);
        String needle = (search != null && !search.isBlank())
                ? search.trim().toLowerCase(Locale.ROOT)
                : null;

        Map<Long, SupplierOutstandingDto> rollupBySupplier = indexById(
                outstandingSummaryService.getSummary(null, false, null, null, true));

        List<Supplier> suppliers = supplierRepository.findAll();
        LocalDate today = LocalDate.now();

        List<MisSupplierListItemDto> rows = new ArrayList<>();
        for (Supplier s : suppliers) {
            MisSupplierListItemDto row = buildRow(s, rollupBySupplier.get(s.getSupplierId()), today);

            if (wantedStatus != null && !wantedStatus.equalsIgnoreCase(row.getStatus())) {
                continue;
            }
            if (startDate != null && (row.getLastTransactionDate() == null
                    || row.getLastTransactionDate().isBefore(startDate))) {
                continue;
            }
            if (endDate != null && (row.getLastTransactionDate() == null
                    || row.getLastTransactionDate().isAfter(endDate))) {
                continue;
            }
            if (!matchesSearch(row, needle)) {
                continue;
            }
            rows.add(row);
        }

        rows.sort(Comparator.comparing(MisSupplierListItemDto::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return rows;
    }

    @Override
    public MisSupplierDetailDto getSupplierDetail(Long supplierId) {
        log.info("MIS: GET /suppliers/{}", supplierId);
        Supplier supplier = requireSupplier(supplierId);
        SupplierOutstandingDto rollup = outstandingSummaryService
                .getSummary(null, false, null, null, true).stream()
                .filter(r -> supplierId.equals(r.getSupplierId()))
                .findFirst()
                .orElse(null);
        MisSupplierListItemDto base = buildRow(supplier, rollup, LocalDate.now());

        return MisSupplierDetailDto.builder()
                .id(base.getId())
                .supplierId(base.getSupplierId())
                .name(base.getName())
                .phone(base.getPhone())
                .email(base.getEmail())
                .address(base.getAddress())
                .status(base.getStatus())
                .outstandingBalance(base.getOutstandingBalance())
                .overdue(base.isOverdue())
                .lastTransactionDate(base.getLastTransactionDate())
                .totalPos(base.getTotalPos())
                .totalGrns(base.getTotalGrns())
                .registrationId(base.getRegistrationId())
                .registrationDate(base.getRegistrationDate())
                .repName(supplier.getRepName())
                .repContactNo(supplier.getRepContactNo())
                .bankDetails(supplier.getBankDetails())
                .vatStatus(supplier.getVatStatus())
                .build();
    }

    @Override
    public List<MisSupplierTransactionDto> getSupplierTransactions(Long supplierId, String search) {
        log.info("MIS: GET /suppliers/{}/transactions search={}", supplierId, search);
        requireSupplier(supplierId);

        // Delegate to the finance ledger service — it already produces every
        // line item we need and handles the running-balance arithmetic.
        List<LedgerEntryDto> entries = supplierLedgerService.getLedger(supplierId, null, null, null, null);

        String needle = (search != null && !search.isBlank())
                ? search.trim().toLowerCase(Locale.ROOT)
                : null;

        return entries.stream()
                .filter(e -> matchesLedger(e, needle))
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MisSupplierPoGrnDto> getSupplierPoGrn(Long supplierId) {
        log.info("MIS: GET /suppliers/{}/po-grn", supplierId);
        requireSupplier(supplierId);

        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(supplierId);
        // Index GRNs by PO so we can stitch in delivery info on a single pass.
        Map<Long, Grn> grnByPo = grnRepository.findBySupplierId(supplierId).stream()
                .filter(g -> g.getPoId() != null)
                // If two GRNs share a PO (split deliveries), keep the most recent one.
                .collect(Collectors.toMap(Grn::getPoId, g -> g, (a, b) -> {
                    if (a.getReceivedDate() == null) return b;
                    if (b.getReceivedDate() == null) return a;
                    return a.getReceivedDate().isAfter(b.getReceivedDate()) ? a : b;
                }));

        List<MisSupplierPoGrnDto> rows = new ArrayList<>();
        for (PurchaseOrder po : pos) {
            rows.add(buildPoGrnRow(po, grnByPo.get(po.getPoId())));
        }

        rows.sort(Comparator.comparing(MisSupplierPoGrnDto::getPoDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    private MisSupplierListItemDto buildRow(Supplier s, SupplierOutstandingDto rollup, LocalDate today) {
        Long id = s.getSupplierId();
        BigDecimal outstanding = rollup != null ? nz(rollup.getTotalOutstanding()) : BigDecimal.ZERO;
        BigDecimal overdueAmt = rollup != null ? nz(rollup.getOverdueAmount()) : BigDecimal.ZERO;
        LocalDate lastTx = rollup != null ? rollup.getLastTransaction() : computeLastTransaction(id);

        long totalGrns = grnRepository.findBySupplierId(id).size();
        long totalPos = purchaseOrderRepository.findBySupplierId(id).size();

        return MisSupplierListItemDto.builder()
                .id(displayId(id))
                .supplierId(id)
                .name(s.getName())
                .phone(s.getContactNumber())
                .email(s.getEmail())
                .address(s.getAddress())
                .status(deriveStatus(id, today))
                .outstandingBalance(outstanding)
                .overdue(overdueAmt.signum() > 0)
                .lastTransactionDate(lastTx)
                .totalPos(totalPos)
                .totalGrns(totalGrns)
                .registrationId(deriveRegistrationId(id, s.getCreatedAt() != null
                        ? s.getCreatedAt().toLocalDate() : null))
                .registrationDate(s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate() : null)
                .build();
    }

    private MisSupplierPoGrnDto buildPoGrnRow(PurchaseOrder po, Grn grn) {
        ProductSummary summary = summarisePo(po);
        boolean delivered = grn != null;

        BigDecimal qty = BigDecimal.ZERO;
        if (delivered && grn.getGrnItems() != null && !grn.getGrnItems().isEmpty()) {
            for (GrnItem gi : grn.getGrnItems()) {
                qty = qty.add(nz(gi.getReceivedQuantity()));
            }
        } else if (po.getNumberOfItems() != null) {
            qty = BigDecimal.valueOf(po.getNumberOfItems());
        }

        BigDecimal amount = delivered ? nz(grn.getTotal()) : nz(po.getTotalCost());

        return MisSupplierPoGrnDto.builder()
                .poNo("PO-" + po.getPoId())
                .poDate(po.getEstimatedDeliveryDate())
                .grnNo(delivered ? "GRN-" + grn.getGrnId() : "—")
                .grnDate(delivered && grn.getReceivedDate() != null
                        ? grn.getReceivedDate().toLocalDate() : null)
                .productSummary(summary.label)
                .qty(qty)
                .unit(summary.unit)
                .amount(amount)
                .status(delivered ? "Delivered" : "Pending")
                .build();
    }

    /** First raw-material name + "+N more" when extra items exist. */
    private ProductSummary summarisePo(PurchaseOrder po) {
        List<PurchaseOrderItem> items = po.getPurchaseOrderItems();
        if (items == null || items.isEmpty()) {
            return new ProductSummary("—", null);
        }
        PurchaseOrderItem first = items.get(0);
        String firstName = "Item";
        String unit = first.getUnitOfMeasure();
        RawMaterial rm = first.getRawMaterial();
        if (rm != null && rm.getMaterialName() != null && !rm.getMaterialName().isBlank()) {
            firstName = rm.getMaterialName();
            if (unit == null || unit.isBlank()) {
                unit = rm.getUnitOfMeasure();
            }
        }
        int extra = items.size() - 1;
        String label = extra > 0 ? firstName + " +" + extra + " more" : firstName;
        return new ProductSummary(label, unit);
    }

    private MisSupplierTransactionDto toTransactionDto(LedgerEntryDto e) {
        return MisSupplierTransactionDto.builder()
                .id(e.getId())
                .date(e.getDate())
                .ref(e.getRef())
                .type(e.getType())
                .description(e.getDescription())
                .debit(e.getDebit())
                .credit(e.getCredit())
                .balance(e.getBalance())
                .status(e.getStatus())
                .build();
    }

    /** Derive {@code Active} / {@code Inactive} from recent activity. */
    String deriveStatus(Long supplierId, LocalDate today) {
        return isActive(supplierId, today) ? STATUS_ACTIVE : STATUS_INACTIVE;
    }

    private boolean isActive(Long supplierId, LocalDate today) {
        LocalDate threshold = today.minusDays(ACTIVE_THRESHOLD_DAYS);
        for (Grn g : grnRepository.findBySupplierId(supplierId)) {
            if (g.getReceivedDate() != null
                    && !g.getReceivedDate().toLocalDate().isBefore(threshold)) {
                return true;
            }
        }
        for (SupplierPayment p : paymentRepository.findBySupplierId(supplierId)) {
            if (p.getPaymentDate() != null && !p.getPaymentDate().isBefore(threshold)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Synthesise {@code REG-{year}-{0000-padded id}} when the schema lacks a
     * registration column. Public for unit testing.
     */
    static String deriveRegistrationId(Long supplierId, LocalDate createdAt) {
        int year = createdAt != null ? createdAt.getYear() : LocalDate.now().getYear();
        long id = supplierId != null ? supplierId : 0L;
        return String.format(Locale.ROOT, "REG-%d-%04d", year, id);
    }

    static String displayId(Long supplierId) {
        return String.format(Locale.ROOT, "SUP-%03d", supplierId != null ? supplierId : 0L);
    }

    /**
     * Recompute {@code lastTransaction} when no rollup row is available
     * (i.e. supplier has zero outstanding GRNs but may still have payments).
     */
    private LocalDate computeLastTransaction(Long supplierId) {
        LocalDate latest = null;
        for (Grn g : grnRepository.findBySupplierId(supplierId)) {
            if (g.getReceivedDate() != null) {
                LocalDate d = g.getReceivedDate().toLocalDate();
                if (latest == null || d.isAfter(latest)) {
                    latest = d;
                }
            }
        }
        for (SupplierPayment p : paymentRepository.findBySupplierId(supplierId)) {
            if (p.getPaymentDate() != null
                    && (latest == null || p.getPaymentDate().isAfter(latest))) {
                latest = p.getPaymentDate();
            }
        }
        return latest;
    }

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        Set<String> allowed = Set.of(STATUS_ACTIVE, STATUS_INACTIVE, STATUS_ALL);
        boolean match = allowed.stream().anyMatch(a -> a.equalsIgnoreCase(status));
        if (!match) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status '" + status + "'. Allowed: Active, Inactive, All.");
        }
    }

    /** {@code "All"} / {@code null} → no filter; otherwise return canonical case. */
    private String normaliseStatusFilter(String status) {
        if (status == null || status.isBlank() || STATUS_ALL.equalsIgnoreCase(status)) {
            return null;
        }
        return STATUS_ACTIVE.equalsIgnoreCase(status) ? STATUS_ACTIVE : STATUS_INACTIVE;
    }

    private boolean matchesSearch(MisSupplierListItemDto row, String needle) {
        if (needle == null) {
            return true;
        }
        if (containsIgnoreCase(row.getName(), needle)) return true;
        if (containsIgnoreCase(row.getId(), needle)) return true;
        if (containsIgnoreCase(row.getEmail(), needle)) return true;
        return false;
    }

    private boolean matchesLedger(LedgerEntryDto e, String needle) {
        if (needle == null) {
            return true;
        }
        return containsIgnoreCase(e.getRef(), needle)
                || containsIgnoreCase(e.getDescription(), needle);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private Supplier requireSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierId is required");
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Supplier not found: " + supplierId));
    }

    private Map<Long, SupplierOutstandingDto> indexById(List<SupplierOutstandingDto> rollups) {
        Map<Long, SupplierOutstandingDto> map = new HashMap<>();
        for (SupplierOutstandingDto r : rollups) {
            if (r.getSupplierId() != null) {
                map.put(r.getSupplierId(), r);
            }
        }
        return map;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Tiny holder for the {@link #summarisePo(PurchaseOrder)} result. */
    private record ProductSummary(String label, String unit) { }
}
