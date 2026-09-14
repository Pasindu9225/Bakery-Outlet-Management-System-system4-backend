package com.plover.backerymanagmentsystem.mis.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.mis.dto.CategoryBreakdownDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchaseRecordDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchasingDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchasingSummaryDto;
import com.plover.backerymanagmentsystem.mis.dto.SupplierSpendDto;
import com.plover.backerymanagmentsystem.mis.dto.TrendPointDto;
import com.plover.backerymanagmentsystem.mis.service.PurchasingTrendsService;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link PurchasingTrendsService}.
 *
 * <h3>Source-walking strategy</h3>
 * <p>The service loads every PO + GRN once per request, joins them in
 * memory, then derives all five widgets (summary, trend, category,
 * supplier, drill-down) from the same record list. This mirrors
 * {@link WastageReportServiceImpl}'s approach and avoids spawning bespoke
 * aggregation queries — the dataset for purchasing is small enough that
 * one full scan is acceptable, and keeping a single source of truth makes
 * the cross-widget numbers consistent (e.g. category totals == sum of
 * line totals over the same record list).</p>
 *
 * <h3>"PO date" proxy</h3>
 * <p>{@link PurchaseOrder} has no {@code created_at} column today, so we
 * use {@link PurchaseOrder#getEstimatedDeliveryDate()} as the "PO date" for
 * trend bucketing AND for the date column in the drill-down table. When a
 * real created-at column lands, swap to it and remove this Javadoc.</p>
 *
 * <h3>Outlet filter</h3>
 * <p>POs are placed centrally; there is no PO→outlet relationship. The
 * {@code outletId} parameter is therefore accepted but ignored, and a
 * {@code dataLimitation="outlet-filter-noop"} flag is added to the envelope
 * so the UI can show a footnote.</p>
 *
 * <h3>Status derivation</h3>
 * <p>{@code PurchaseOrder.status} is the raw approval state
 * ({@code PENDING}/{@code APPROVED}). Whether the goods have actually
 * arrived is recorded on the linked GRN. We surface a friendlier
 * {@code "Received"}/{@code "Partial"}/{@code "Pending"} label derived
 * from the GRN's {@link GrnStatus}, falling back to {@code "Pending"} when
 * no GRN exists.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchasingTrendsServiceImpl implements PurchasingTrendsService {

    public static final String STATUS_RECEIVED = "Received";
    public static final String STATUS_PARTIAL = "Partial";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_CANCELLED = "Cancelled";

    public static final String UNCATEGORIZED = "Uncategorized";
    public static final String CENTRAL_STORE = "Central Store";

    private static final int DAILY_BUCKETS = 7;
    private static final int MONTHLY_BUCKETS = 8;
    private static final int YEARLY_BUCKETS = 5;
    private static final int DEFAULT_RECORDS_LIMIT = 200;

    private static final DateTimeFormatter DAILY_LABEL =
            DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    private static final DateTimeFormatter MONTHLY_LABEL =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GrnRepository grnRepository;
    private final SupplierRepository supplierRepository;

    // ──────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────

    @Override
    public PurchasingDashboardDto getDashboard(LocalDate startDate,
                                               LocalDate endDate,
                                               Long supplierId,
                                               String category,
                                               Long outletId,
                                               Granularity granularity) {
        validateDateRange(startDate, endDate);
        Granularity effective = granularity != null ? granularity : Granularity.MONTHLY;

        log.info("MIS purchasing dashboard: from={} to={} supplierId={} category={} outletId={} granularity={}",
                startDate, endDate, supplierId, category, outletId, effective);

        Lookups lookups = loadLookups();
        List<PurchaseRecordDto> all = collectAllRecords(lookups);
        List<PurchaseRecordDto> filtered = filterRecords(all, startDate, endDate,
                supplierId, category, /* search */ null);

        PurchasingSummaryDto summary = buildSummary(filtered);
        List<TrendPointDto> trend = buildTrend(all, startDate, endDate,
                supplierId, category, effective);
        List<CategoryBreakdownDto> categoryBreakdown = buildCategoryBreakdown(filtered);
        List<SupplierSpendDto> supplierRanking = buildSupplierRanking(filtered);

        List<PurchaseRecordDto> sample = filtered.stream()
                .sorted(Comparator.comparing(PurchaseRecordDto::getPoDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(DEFAULT_RECORDS_LIMIT)
                .toList();

        PurchasingDashboardDto.PurchasingDashboardDtoBuilder builder = PurchasingDashboardDto.builder()
                .from(startDate)
                .to(endDate)
                .granularity(effective.name())
                .summary(summary)
                .trend(trend)
                .categoryBreakdown(categoryBreakdown)
                .supplierRanking(supplierRanking)
                .records(sample);

        if (outletId != null) {
            builder.dataLimitation("outlet-filter-noop")
                    .dataLimitationReason("Outlet filter is ignored: purchase orders are placed centrally and have no per-outlet attribution today.");
        }
        return builder.build();
    }

    @Override
    public List<TrendPointDto> getTrend(LocalDate startDate,
                                        LocalDate endDate,
                                        Long supplierId,
                                        String category,
                                        Long outletId,
                                        Granularity granularity) {
        validateDateRange(startDate, endDate);
        Granularity effective = granularity != null ? granularity : Granularity.MONTHLY;
        log.info("MIS purchasing trend: from={} to={} supplierId={} category={} granularity={}",
                startDate, endDate, supplierId, category, effective);

        Lookups lookups = loadLookups();
        List<PurchaseRecordDto> all = collectAllRecords(lookups);
        return buildTrend(all, startDate, endDate, supplierId, category, effective);
    }

    @Override
    public Page<PurchaseRecordDto> getRecords(LocalDate startDate,
                                              LocalDate endDate,
                                              Long supplierId,
                                              String category,
                                              Long outletId,
                                              String search,
                                              Pageable pageable) {
        validateDateRange(startDate, endDate);
        log.info("MIS purchasing records: from={} to={} supplierId={} category={} search={} page={} size={}",
                startDate, endDate, supplierId, category, search,
                pageable.getPageNumber(), pageable.getPageSize());

        Lookups lookups = loadLookups();
        List<PurchaseRecordDto> filtered = filterRecords(
                collectAllRecords(lookups), startDate, endDate, supplierId, category, search);
        filtered.sort(Comparator.comparing(PurchaseRecordDto::getPoDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int total = filtered.size();
        int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        List<PurchaseRecordDto> slice = from >= to ? List.of() : filtered.subList(from, to);
        return new PageImpl<>(slice, pageable, total);
    }

    // ──────────────────────────────────────────────────────────────────
    // Validation
    // ──────────────────────────────────────────────────────────────────

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate != null && endDate.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "endDate cannot be in the future");
        }
        if (startDate != null && startDate.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate cannot be in the future");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be on or before endDate");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Collection
    // ──────────────────────────────────────────────────────────────────

    /** One-shot fetch of everything we need. */
    private record Lookups(Map<Long, Supplier> suppliersById,
                           Map<Long, Grn> grnByPoId) { }

    private Lookups loadLookups() {
        Map<Long, Supplier> suppliers = new HashMap<>();
        for (Supplier s : supplierRepository.findAll()) {
            if (s.getSupplierId() != null) suppliers.put(s.getSupplierId(), s);
        }
        Map<Long, Grn> grnByPo = new HashMap<>();
        for (Grn g : grnRepository.findAll()) {
            if (g.getPoId() == null) continue;
            // If the PO has multiple GRNs (split deliveries), keep the most
            // recent / most-complete one for status derivation. Prefer
            // RECEIVED > PARTIAL > PENDING > CANCELLED.
            Grn existing = grnByPo.get(g.getPoId());
            if (existing == null || compareGrnPriority(g, existing) > 0) {
                grnByPo.put(g.getPoId(), g);
            }
        }
        return new Lookups(suppliers, grnByPo);
    }

    /** Higher rank wins. RECEIVED > PARTIAL > PENDING > CANCELLED > null. */
    private static int compareGrnPriority(Grn a, Grn b) {
        return Integer.compare(grnRank(a), grnRank(b));
    }

    private static int grnRank(Grn g) {
        if (g == null || g.getGrnStatus() == null) return -1;
        return switch (g.getGrnStatus()) {
            case RECEIVED -> 3;
            case PARTIAL -> 2;
            case PENDING -> 1;
            case CANCELLED -> 0;
        };
    }

    /** Walk every PO's line items and emit one drill-down row per line. */
    private List<PurchaseRecordDto> collectAllRecords(Lookups lookups) {
        List<PurchaseRecordDto> rows = new ArrayList<>();
        for (PurchaseOrder po : purchaseOrderRepository.findAll()) {
            String poNo = "PO-" + po.getPoId();
            String supplierName = supplierName(po.getSupplierId(), lookups.suppliersById());
            Grn grn = po.getPoId() != null ? lookups.grnByPoId().get(po.getPoId()) : null;
            String grnNo = grn != null ? "GRN-" + grn.getGrnId() : "—";
            String status = deriveStatus(po, grn);

            List<PurchaseOrderItem> items = po.getPurchaseOrderItems();
            if (items == null || items.isEmpty()) {
                // No line items — still surface the header row so the PO appears
                // in totals. Use the PO's totalCost as the line total, with no
                // qty / category / product.
                rows.add(PurchaseRecordDto.builder()
                        .poNo(poNo)
                        .poId(po.getPoId())
                        .poDate(po.getEstimatedDeliveryDate())
                        .supplier(supplierName)
                        .supplierId(po.getSupplierId())
                        .category(UNCATEGORIZED)
                        .outlet(CENTRAL_STORE)
                        .outletId(null)
                        .product("—")
                        .productId(null)
                        .qty(BigDecimal.ZERO)
                        .unit(null)
                        .unitCost(BigDecimal.ZERO)
                        .totalCost(nz(po.getTotalCost()))
                        .grnNo(grnNo)
                        .status(status)
                        .build());
                continue;
            }
            for (PurchaseOrderItem item : items) {
                rows.add(toRecord(po, item, grn, supplierName, poNo, grnNo, status));
            }
        }
        return rows;
    }

    private PurchaseRecordDto toRecord(PurchaseOrder po, PurchaseOrderItem item, Grn grn,
                                       String supplierName, String poNo, String grnNo,
                                       String status) {
        RawMaterial rm = item.getRawMaterial();
        String productName = rm != null && rm.getMaterialName() != null
                ? rm.getMaterialName()
                : (item.getRawMaterialId() != null ? "RM-" + item.getRawMaterialId() : "Item");
        Long productId = rm != null && rm.getId() != null
                ? rm.getId()
                : (item.getRawMaterialId() != null ? item.getRawMaterialId().longValue() : null);

        String category = rm != null && rm.getCategory() != null && !rm.getCategory().isBlank()
                ? rm.getCategory() : UNCATEGORIZED;

        String unit = item.getUnitOfMeasure();
        if ((unit == null || unit.isBlank()) && rm != null) {
            unit = rm.getUnitOfMeasure();
        }

        // Prefer received qty (truth post-delivery) over required qty (intent).
        BigDecimal qty = item.getReceivedQty() != null
                ? BigDecimal.valueOf(item.getReceivedQty())
                : (item.getRequiredQty() != null
                        ? BigDecimal.valueOf(item.getRequiredQty())
                        : BigDecimal.ZERO);

        BigDecimal totalCost = item.getActualCost() != null
                ? nz(item.getActualCost()) : nz(item.getEstimatedCost());
        BigDecimal unitCost = qty.signum() > 0
                ? totalCost.divide(qty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PurchaseRecordDto.builder()
                .poNo(poNo)
                .poId(po.getPoId())
                .poDate(po.getEstimatedDeliveryDate())
                .supplier(supplierName)
                .supplierId(po.getSupplierId())
                .category(category)
                .outlet(CENTRAL_STORE)
                .outletId(null)
                .product(productName)
                .productId(productId)
                .qty(qty)
                .unit(unit)
                .unitCost(unitCost.setScale(2, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .grnNo(grnNo)
                .status(status)
                .build();
    }

    private static String supplierName(Long supplierId, Map<Long, Supplier> suppliers) {
        if (supplierId == null) return "Unknown Supplier";
        Supplier s = suppliers.get(supplierId);
        return s != null && s.getName() != null ? s.getName() : "Supplier #" + supplierId;
    }

    /** Friendlier display label derived from PO + GRN state. */
    static String deriveStatus(PurchaseOrder po, Grn grn) {
        if (po != null && "CANCELLED".equalsIgnoreCase(po.getStatus())) {
            return STATUS_CANCELLED;
        }
        if (grn == null || grn.getGrnStatus() == null) return STATUS_PENDING;
        return switch (grn.getGrnStatus()) {
            case RECEIVED -> STATUS_RECEIVED;
            case PARTIAL -> STATUS_PARTIAL;
            case CANCELLED -> STATUS_CANCELLED;
            case PENDING -> STATUS_PENDING;
        };
    }

    // ──────────────────────────────────────────────────────────────────
    // Filtering / aggregation
    // ──────────────────────────────────────────────────────────────────

    private List<PurchaseRecordDto> filterRecords(List<PurchaseRecordDto> all,
                                                  LocalDate startDate, LocalDate endDate,
                                                  Long supplierId, String category,
                                                  String search) {
        String wantedCategory = normaliseCategoryFilter(category);
        String needle = (search != null && !search.isBlank())
                ? search.trim().toLowerCase(Locale.ROOT) : null;

        List<PurchaseRecordDto> out = new ArrayList<>(all.size());
        for (PurchaseRecordDto r : all) {
            if (!withinRange(r.getPoDate(), startDate, endDate)) continue;
            if (supplierId != null && !supplierId.equals(r.getSupplierId())) continue;
            if (wantedCategory != null
                    && !wantedCategory.equalsIgnoreCase(r.getCategory())) continue;
            if (needle != null && !matchesSearch(r, needle)) continue;
            out.add(r);
        }
        return out;
    }

    private static String normaliseCategoryFilter(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if ("All".equalsIgnoreCase(raw) || "All Categories".equalsIgnoreCase(raw)) return null;
        return raw.trim();
    }

    private static boolean matchesSearch(PurchaseRecordDto r, String needle) {
        return containsIgnoreCase(r.getPoNo(), needle)
                || containsIgnoreCase(r.getSupplier(), needle)
                || containsIgnoreCase(r.getProduct(), needle);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean withinRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return from == null && to == null;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    // ──────────────────────────────────────────────────────────────────
    // Summary
    // ──────────────────────────────────────────────────────────────────

    private PurchasingSummaryDto buildSummary(List<PurchaseRecordDto> rows) {
        // POs are repeated once per line item — collapse to unique poIds for
        // the count / pending tally so we report PO-level metrics rather than
        // line-item totals.
        Map<Long, BigDecimal> spendByPo = new LinkedHashMap<>();
        Map<Long, String> statusByPo = new HashMap<>();
        for (PurchaseRecordDto r : rows) {
            Long key = r.getPoId() != null ? r.getPoId() : -1L;
            spendByPo.merge(key, nz(r.getTotalCost()), BigDecimal::add);
            statusByPo.putIfAbsent(key, r.getStatus());
        }

        long totalPos = spendByPo.size();
        BigDecimal totalSpend = BigDecimal.ZERO;
        for (BigDecimal v : spendByPo.values()) totalSpend = totalSpend.add(v);

        BigDecimal avg = totalPos > 0
                ? totalSpend.divide(BigDecimal.valueOf(totalPos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long pending = statusByPo.values().stream()
                .filter(s -> !STATUS_RECEIVED.equalsIgnoreCase(s)
                        && !STATUS_CANCELLED.equalsIgnoreCase(s))
                .count();

        return PurchasingSummaryDto.builder()
                .totalPos(totalPos)
                .totalSpend(totalSpend.setScale(2, RoundingMode.HALF_UP))
                .avgOrderValue(avg)
                .pendingPos(pending)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // Trend bucketing
    // ──────────────────────────────────────────────────────────────────

    private List<TrendPointDto> buildTrend(List<PurchaseRecordDto> all,
                                           LocalDate startDate, LocalDate endDate,
                                           Long supplierId, String category,
                                           Granularity granularity) {
        // Apply the same filters as the dashboard EXCEPT the date window —
        // the trend itself defines its own (rolling) window per granularity.
        // We keep the date filter when explicitly supplied so the user can
        // narrow, but ignore it for the lastYear lookback (we still need the
        // prior-year buckets even when startDate is recent).
        List<PurchaseRecordDto> filtered = filterRecords(
                all, /* startDate */ null, /* endDate */ null,
                supplierId, category, /* search */ null);

        LocalDate anchor = endDate != null ? endDate : LocalDate.now();
        return switch (granularity) {
            case DAILY -> buildDailyTrend(filtered, anchor, startDate);
            case MONTHLY -> buildMonthlyTrend(filtered, anchor, startDate);
            case YEARLY -> buildYearlyTrend(filtered, anchor, startDate);
        };
    }

    private List<TrendPointDto> buildDailyTrend(List<PurchaseRecordDto> rows,
                                                LocalDate anchor, LocalDate startDate) {
        // Window: anchor back DAILY_BUCKETS-1 days, clamped to startDate when
        // provided so we never project beyond the user's request.
        LocalDate desiredStart = anchor.minusDays(DAILY_BUCKETS - 1L);
        LocalDate windowStart = startDate != null && startDate.isAfter(desiredStart)
                ? startDate : desiredStart;
        if (windowStart.isAfter(anchor)) windowStart = anchor;

        // Pre-bucket all rows by day for both windows in one scan.
        Map<LocalDate, BigDecimal> spendByDay = new HashMap<>();
        for (PurchaseRecordDto r : rows) {
            if (r.getPoDate() == null) continue;
            spendByDay.merge(r.getPoDate(), nz(r.getTotalCost()), BigDecimal::add);
        }

        List<TrendPointDto> series = new ArrayList<>();
        for (LocalDate day = windowStart; !day.isAfter(anchor); day = day.plusDays(1)) {
            LocalDate prior = day.minusYears(1);
            series.add(TrendPointDto.builder()
                    .label(day.format(DAILY_LABEL))
                    .thisYear(spendByDay.getOrDefault(day, BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP))
                    .lastYear(spendByDay.getOrDefault(prior, BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return series;
    }

    private List<TrendPointDto> buildMonthlyTrend(List<PurchaseRecordDto> rows,
                                                  LocalDate anchor, LocalDate startDate) {
        YearMonth anchorMonth = YearMonth.from(anchor);
        YearMonth desiredStartMonth = anchorMonth.minusMonths(MONTHLY_BUCKETS - 1L);
        YearMonth windowStart = startDate != null
                && YearMonth.from(startDate).isAfter(desiredStartMonth)
                ? YearMonth.from(startDate) : desiredStartMonth;
        if (windowStart.isAfter(anchorMonth)) windowStart = anchorMonth;

        Map<YearMonth, BigDecimal> spendByMonth = new HashMap<>();
        for (PurchaseRecordDto r : rows) {
            if (r.getPoDate() == null) continue;
            spendByMonth.merge(YearMonth.from(r.getPoDate()),
                    nz(r.getTotalCost()), BigDecimal::add);
        }

        List<TrendPointDto> series = new ArrayList<>();
        for (YearMonth m = windowStart; !m.isAfter(anchorMonth); m = m.plusMonths(1)) {
            YearMonth prior = m.minusYears(1);
            series.add(TrendPointDto.builder()
                    .label(m.atDay(1).format(MONTHLY_LABEL))
                    .thisYear(spendByMonth.getOrDefault(m, BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP))
                    .lastYear(spendByMonth.getOrDefault(prior, BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return series;
    }

    private List<TrendPointDto> buildYearlyTrend(List<PurchaseRecordDto> rows,
                                                 LocalDate anchor, LocalDate startDate) {
        int anchorYear = anchor.getYear();
        int desiredStartYear = anchorYear - (YEARLY_BUCKETS - 1);
        int windowStartYear = startDate != null && startDate.getYear() > desiredStartYear
                ? startDate.getYear() : desiredStartYear;
        if (windowStartYear > anchorYear) windowStartYear = anchorYear;

        Map<Integer, BigDecimal> spendByYear = new HashMap<>();
        for (PurchaseRecordDto r : rows) {
            if (r.getPoDate() == null) continue;
            spendByYear.merge(r.getPoDate().getYear(),
                    nz(r.getTotalCost()), BigDecimal::add);
        }

        List<TrendPointDto> series = new ArrayList<>();
        for (int y = windowStartYear; y <= anchorYear; y++) {
            series.add(TrendPointDto.builder()
                    .label(String.valueOf(y))
                    .thisYear(spendByYear.getOrDefault(y, BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP))
                    .lastYear(spendByYear.getOrDefault(y - 1, BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return series;
    }

    // ──────────────────────────────────────────────────────────────────
    // Category & supplier breakdowns
    // ──────────────────────────────────────────────────────────────────

    private List<CategoryBreakdownDto> buildCategoryBreakdown(List<PurchaseRecordDto> rows) {
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        for (PurchaseRecordDto r : rows) {
            String key = r.getCategory() != null ? r.getCategory() : UNCATEGORIZED;
            sums.merge(key, nz(r.getTotalCost()), BigDecimal::add);
        }
        List<CategoryBreakdownDto> out = new ArrayList<>();
        sums.forEach((label, value) -> out.add(CategoryBreakdownDto.builder()
                .label(label)
                .value(value.setScale(2, RoundingMode.HALF_UP))
                .build()));
        out.sort(Comparator.<CategoryBreakdownDto, BigDecimal>comparing(
                CategoryBreakdownDto::getValue, Comparator.reverseOrder()));
        return out;
    }

    private List<SupplierSpendDto> buildSupplierRanking(List<PurchaseRecordDto> rows) {
        record Bucket(Long id, String name, BigDecimal spend) { }
        Map<Long, Bucket> sums = new LinkedHashMap<>();
        for (PurchaseRecordDto r : rows) {
            Long id = r.getSupplierId() != null ? r.getSupplierId() : -1L;
            String name = r.getSupplier() != null ? r.getSupplier() : "Unknown Supplier";
            Bucket existing = sums.get(id);
            BigDecimal spend = nz(r.getTotalCost());
            if (existing == null) {
                sums.put(id, new Bucket(r.getSupplierId(), name, spend));
            } else {
                sums.put(id, new Bucket(existing.id(), existing.name(),
                        existing.spend().add(spend)));
            }
        }
        List<SupplierSpendDto> out = new ArrayList<>();
        for (Bucket b : sums.values()) {
            out.add(SupplierSpendDto.builder()
                    .supplierId(b.id())
                    .supplierName(b.name())
                    .totalSpend(b.spend().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        out.sort(Comparator.<SupplierSpendDto, BigDecimal>comparing(
                SupplierSpendDto::getTotalSpend, Comparator.reverseOrder()));
        return out;
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
