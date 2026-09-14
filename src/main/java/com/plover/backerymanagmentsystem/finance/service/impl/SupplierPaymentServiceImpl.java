package com.plover.backerymanagmentsystem.finance.service.impl;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.finance.dto.CreatePaymentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.CreatePaymentRequestDto.AllocationDto;
import com.plover.backerymanagmentsystem.finance.dto.OutstandingGrnDto;
import com.plover.backerymanagmentsystem.finance.dto.PaymentResponseDto;
import com.plover.backerymanagmentsystem.finance.dto.PaymentResponseDto.AllocationResponseDto;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.model.SupplierPaymentAllocation;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentAllocationRepository;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.finance.service.SupplierPaymentService;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link SupplierPaymentService}. Handles
 * outstanding-GRN projection, payment creation with per-allocation validation,
 * and payment listing/lookup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    /** Soft-limit window in days for the {@code paymentDate} forward bound. */
    static final int FUTURE_DATE_LIMIT_DAYS = 30;

    /** Soft-limit window in days for marking a GRN as overdue. */
    static final int OVERDUE_THRESHOLD_DAYS = 30;

    /** Canonical payment-method tokens accepted by the service. */
    static final Set<String> ALLOWED_METHODS = Set.of(
            "BANK_TRANSFER", "CASH", "CHEQUE", "CREDIT_CARD", "ONLINE_TRANSFER");

    private final SupplierPaymentRepository paymentRepository;
    private final SupplierPaymentAllocationRepository allocationRepository;
    private final GrnRepository grnRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingGrnDto> getOutstandingGrns(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("supplierId is required");
        }
        log.info("Finance: fetching outstanding GRNs for supplier {}", supplierId);

        List<Grn> grns = grnRepository.findBySupplierId(supplierId);
        LocalDate today = LocalDate.now();

        List<OutstandingGrnDto> result = new ArrayList<>();
        for (Grn g : grns) {
            if (g.getGrnStatus() != GrnStatus.RECEIVED && g.getGrnStatus() != GrnStatus.PARTIAL) {
                continue;
            }
            BigDecimal total = nz(g.getTotal());
            BigDecimal paid = nz(allocationRepository.sumAllocatedByGrnId(g.getGrnId()));
            BigDecimal outstanding = total.subtract(paid);
            if (outstanding.signum() <= 0) {
                continue;
            }

            LocalDate received = g.getReceivedDate() != null ? g.getReceivedDate().toLocalDate() : null;
            LocalDate due = received != null ? received.plusDays(OVERDUE_THRESHOLD_DAYS) : null;
            String status = (due != null && today.isAfter(due)) ? "Overdue" : "Pending";

            String description = (g.getInvoiceNumber() != null && !g.getInvoiceNumber().isBlank())
                    ? "Goods Received - " + g.getInvoiceNumber()
                    : "Goods Received";

            result.add(OutstandingGrnDto.builder()
                    .grnId(g.getGrnId())
                    .ref("GRN-" + g.getGrnId())
                    .description(description)
                    .date(received != null ? received.toString() : null)
                    .dueDate(due != null ? due.toString() : null)
                    .amount(total)
                    .paid(paid)
                    .outstanding(outstanding)
                    .status(status)
                    .build());
        }

        // Stable sort: oldest receipt first so overdue invoices show on top in
        // the default UI layout.
        result.sort(Comparator.comparing(
                OutstandingGrnDto::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    @Transactional
    public PaymentResponseDto createPayment(CreatePaymentRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("supplierId is required");
        }
        if (request.getPaymentDate() == null) {
            throw new IllegalArgumentException("paymentDate is required");
        }
        if (request.getAllocations() == null || request.getAllocations().isEmpty()) {
            throw new IllegalArgumentException("at least one allocation is required");
        }

        String method = normalizeMethod(request.getPaymentMethod());

        // Validate forward bound on the payment date.
        LocalDate today = LocalDate.now();
        if (request.getPaymentDate().isAfter(today.plusDays(FUTURE_DATE_LIMIT_DAYS))) {
            throw new IllegalArgumentException(
                    "paymentDate cannot be more than " + FUTURE_DATE_LIMIT_DAYS + " days in the future");
        }

        // Load all referenced GRNs and validate ownership + outstanding amounts.
        List<Long> grnIds = request.getAllocations().stream()
                .map(AllocationDto::getGrnId)
                .collect(Collectors.toList());
        Map<Long, Grn> grnById = grnRepository.findAllById(grnIds).stream()
                .collect(Collectors.toMap(Grn::getGrnId, g -> g));

        BigDecimal totalAllocated = BigDecimal.ZERO;
        LocalDate earliestGrnDate = null;
        for (AllocationDto a : request.getAllocations()) {
            if (a.getGrnId() == null) {
                throw new IllegalArgumentException("allocation grnId is required");
            }
            if (a.getAllocatedAmount() == null || a.getAllocatedAmount().signum() <= 0) {
                throw new IllegalArgumentException("allocation amount must be positive (grnId=" + a.getGrnId() + ")");
            }
            Grn g = grnById.get(a.getGrnId());
            if (g == null) {
                throw new IllegalArgumentException("GRN not found: " + a.getGrnId());
            }
            if (!request.getSupplierId().equals(g.getSupplierId())) {
                throw new IllegalArgumentException(
                        "GRN " + a.getGrnId() + " does not belong to supplier " + request.getSupplierId());
            }
            if (g.getGrnStatus() != GrnStatus.RECEIVED && g.getGrnStatus() != GrnStatus.PARTIAL) {
                throw new IllegalArgumentException(
                        "GRN " + a.getGrnId() + " is not eligible for payment (status=" + g.getGrnStatus() + ")");
            }
            BigDecimal grnTotal = nz(g.getTotal());
            BigDecimal alreadyPaid = nz(allocationRepository.sumAllocatedByGrnId(g.getGrnId()));
            BigDecimal outstanding = grnTotal.subtract(alreadyPaid);
            if (a.getAllocatedAmount().compareTo(outstanding) > 0) {
                throw new IllegalArgumentException(
                        "allocated amount " + a.getAllocatedAmount()
                                + " exceeds outstanding " + outstanding
                                + " for GRN " + a.getGrnId());
            }

            totalAllocated = totalAllocated.add(a.getAllocatedAmount());

            LocalDate gd = g.getReceivedDate() != null ? g.getReceivedDate().toLocalDate() : null;
            if (gd != null && (earliestGrnDate == null || gd.isBefore(earliestGrnDate))) {
                earliestGrnDate = gd;
            }
        }

        // Payment date must not predate the earliest allocated GRN's receipt.
        if (earliestGrnDate != null && request.getPaymentDate().isBefore(earliestGrnDate)) {
            throw new IllegalArgumentException(
                    "paymentDate cannot be before the earliest allocated GRN date " + earliestGrnDate);
        }

        // Build header, then attach lines and persist via cascade.
        String ref = generatePaymentRef(request.getPaymentDate().getYear());
        SupplierPayment payment = SupplierPayment.builder()
                .paymentRef(ref)
                .paymentDate(request.getPaymentDate())
                .supplierId(request.getSupplierId())
                .amount(totalAllocated)
                .paymentMethod(method)
                .status("CLEARED")
                .remarks(request.getRemarks())
                .build();

        List<SupplierPaymentAllocation> lines = new ArrayList<>();
        for (AllocationDto a : request.getAllocations()) {
            lines.add(SupplierPaymentAllocation.builder()
                    .grnId(a.getGrnId())
                    .allocatedAmount(a.getAllocatedAmount())
                    .payment(payment)
                    .build());
        }
        payment.setAllocations(lines);

        SupplierPayment saved = paymentRepository.save(payment);
        log.info("Created payment {} for supplier {} ({} allocations, total={})",
                saved.getPaymentRef(), saved.getSupplierId(), lines.size(), totalAllocated);

        return toResponse(saved, lookupSupplierName(saved.getSupplierId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> listPayments(Long supplierId, String status, LocalDate from, LocalDate to,
            String search) {
        log.info("Finance: list payments supplier={} status={} from={} to={} search={}",
                supplierId, status, from, to, search);
        List<SupplierPayment> all = paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc();

        // Cache supplier names so we don't re-query for each row in a tight loop.
        Map<Long, String> supplierNames = new HashMap<>();

        String search2 = (search != null && !search.isBlank()) ? search.toLowerCase(Locale.ROOT) : null;
        String status2 = (status != null && !status.isBlank()) ? status.trim() : null;

        return all.stream()
                .filter(p -> supplierId == null || supplierId.equals(p.getSupplierId()))
                .filter(p -> from == null || (p.getPaymentDate() != null && !p.getPaymentDate().isBefore(from)))
                .filter(p -> to == null || (p.getPaymentDate() != null && !p.getPaymentDate().isAfter(to)))
                .filter(p -> {
                    if (status2 == null) {
                        return true;
                    }
                    String dispStatus = displayStatus(p.getStatus());
                    return dispStatus.equalsIgnoreCase(status2);
                })
                .map(p -> toResponse(p, supplierNames.computeIfAbsent(p.getSupplierId(), this::lookupSupplierName)))
                .filter(dto -> matchesSearch(dto, search2))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(Long paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        SupplierPayment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return toResponse(p, lookupSupplierName(p.getSupplierId()));
    }

    private boolean matchesSearch(PaymentResponseDto dto, String needle) {
        if (needle == null) {
            return true;
        }
        if (dto.getPaymentRef() != null && dto.getPaymentRef().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (dto.getSupplierName() != null && dto.getSupplierName().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (dto.getRemarks() != null && dto.getRemarks().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (dto.getAllocations() != null) {
            for (AllocationResponseDto a : dto.getAllocations()) {
                if (a.getGrnRef() != null && a.getGrnRef().toLowerCase(Locale.ROOT).contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String lookupSupplierName(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId).map(Supplier::getName).orElse(null);
    }

    private PaymentResponseDto toResponse(SupplierPayment payment, String supplierName) {
        List<AllocationResponseDto> allocs = payment.getAllocations() == null
                ? List.of()
                : payment.getAllocations().stream()
                        .map(a -> AllocationResponseDto.builder()
                                .allocationId(a.getAllocationId())
                                .grnId(a.getGrnId())
                                .grnRef(a.getGrnId() != null ? "GRN-" + a.getGrnId() : null)
                                .allocatedAmount(a.getAllocatedAmount())
                                .build())
                        .collect(Collectors.toList());

        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .paymentRef(payment.getPaymentRef())
                .paymentDate(payment.getPaymentDate())
                .supplierId(payment.getSupplierId())
                .supplierName(supplierName)
                .amount(payment.getAmount())
                .paymentMethod(toDisplayMethod(payment.getPaymentMethod()))
                .status(displayStatus(payment.getStatus()))
                .remarks(payment.getRemarks())
                .createdAt(payment.getCreatedAt())
                .allocations(allocs)
                .build();
    }

    /**
     * Generate a payment reference of the form {@code PMT-YYYY-NNN} using a
     * count of existing entries with the same year prefix plus one.
     */
    String generatePaymentRef(int year) {
        String prefix = String.format("PMT-%d-", year);
        long existing = paymentRepository.countByPaymentRefPrefix(prefix);
        return String.format("%s%03d", prefix, existing + 1);
    }

    /**
     * Normalise a payment-method string from either display form
     * ({@code "Bank Transfer"}) or canonical form ({@code "BANK_TRANSFER"})
     * into the canonical token. Throws when the value is not recognised.
     */
    static String normalizeMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required");
        }
        String token = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!ALLOWED_METHODS.contains(token)) {
            throw new IllegalArgumentException(
                    "unknown paymentMethod '" + raw + "' (allowed: BANK_TRANSFER, CASH, CHEQUE, CREDIT_CARD, ONLINE_TRANSFER)");
        }
        return token;
    }

    /**
     * Convert a canonical payment-method token into its display form, e.g.
     * {@code BANK_TRANSFER} → {@code "Bank Transfer"}. Unknown values pass
     * through unchanged so legacy data does not crash the response.
     */
    static String toDisplayMethod(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String[] parts = stored.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    /** Map persisted status to the display vocabulary used by the UI. */
    static String displayStatus(String stored) {
        if (stored == null) {
            return "Pending";
        }
        return "CLEARED".equalsIgnoreCase(stored) ? "Cleared" : "Pending";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
