package com.plover.backerymanagmentsystem.mis.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionBatch;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.mis.dto.WastageDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageGroupDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageRecordDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageSummaryDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageTrendPointDto;
import com.plover.backerymanagmentsystem.mis.service.WastageReportService;
import com.plover.backerymanagmentsystem.pos.model.OutletReturn;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnItem;
import com.plover.backerymanagmentsystem.pos.model.Return;
import com.plover.backerymanagmentsystem.pos.model.ReturnItem;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.repository.OutletReturnRepository;
import com.plover.backerymanagmentsystem.pos.repository.ReturnRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentReason;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link WastageReportService}.
 *
 * <p>The service streams the underlying entities once per call, builds a
 * single in-memory list of {@link WastageRecordDto rows}, then derives the
 * summary, groupings, and trend series from that list. This avoids
 * duplicating the source-walking logic between the dashboard and the
 * paginated drill-down endpoint, and keeps the queries small enough that
 * we can treat each module's repository as the source of truth without
 * additional bespoke aggregation queries.</p>
 *
 * <h3>Reason taxonomy</h3>
 * <p>Source modules use different vocabularies, so we collapse everything
 * down to a canonical set of {@code EXPIRED} / {@code DAMAGED} /
 * {@code RETURNED} / {@code OTHER}. The mapping is:</p>
 * <ul>
 *   <li>StockAdjust enum — {@link StockAdjustmentReason#EXPIRED} →
 *       EXPIRED; {@link StockAdjustmentReason#DAMAGE} → DAMAGED; everything
 *       else → OTHER (LOSS / COUNTING_ERROR are treated as OTHER because
 *       they are not strictly wastage causes).</li>
 *   <li>ProductionBatch{@code .wastageReason} (free text) — case-insensitive
 *       contains "expir" → EXPIRED; "damag" → DAMAGED; "return" → RETURNED;
 *       otherwise OTHER.</li>
 *   <li>POS {@code Return}{@code .returnReason} — same free-text rules,
 *       but defaults to RETURNED when no keyword matches (since the source
 *       itself is a customer-return event).</li>
 *   <li>{@code OutletReturn}{@code .reason} — keyword match with a
 *       RETURNED default.</li>
 * </ul>
 *
 * <h3>Source coverage</h3>
 * <p>Production batches always contribute a value when the linked product
 * has a {@code unitPrice}; POS returns use the captured {@code unitPrice}
 * on the return item; stock adjustments use the raw material's
 * {@code unitCost}. Outlet returns currently have no per-item price, so
 * they contribute quantity only. When any source contributes records but
 * no value, {@code dataLimitation} is set to {@code "value-not-available"}
 * on the envelope.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WastageReportServiceImpl implements WastageReportService {

    public static final String REASON_EXPIRED = "EXPIRED";
    public static final String REASON_DAMAGED = "DAMAGED";
    public static final String REASON_RETURNED = "RETURNED";
    public static final String REASON_OTHER = "OTHER";

    public static final String SOURCE_PRODUCTION = "PRODUCTION";
    public static final String SOURCE_POS_RETURN = "POS_RETURN";
    public static final String SOURCE_OUTLET_RETURN = "OUTLET_RETURN";
    public static final String SOURCE_STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT";

    private static final int DEFAULT_RECORDS_LIMIT = 200;
    private static final int MAX_PAGE_SIZE = 500;
    private static final int TREND_DAYS = 7;
    private static final DateTimeFormatter TREND_LABEL_FMT =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final ProductionBatchRepository productionBatchRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final ProductRepository productRepository;
    private final OutletRepository outletRepository;
    private final ReturnRepository returnRepository;
    private final OutletReturnRepository outletReturnRepository;
    private final StockAdjustRepository stockAdjustRepository;
    private final SaleRepository saleRepository;

    // ──────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────

    @Override
    public WastageDashboardDto getDashboard(LocalDate startDate,
                                            LocalDate endDate,
                                            Long outletId,
                                            Long productId,
                                            String reason) {
        validateDateRange(startDate, endDate);
        String canonicalReason = normaliseReason(reason);

        log.info("MIS wastage dashboard: from={} to={} outletId={} productId={} reason={}",
                startDate, endDate, outletId, productId, canonicalReason);

        Lookups lookups = loadLookups();
        List<WastageRecordDto> allRecords = collectAllRecords(lookups);

        List<WastageRecordDto> filtered = filterRecords(
                allRecords, startDate, endDate, outletId, productId, canonicalReason);

        WastageSummaryDto summary = buildSummary(filtered, startDate, endDate);
        List<WastageGroupDto> topProducts = groupBy(filtered,
                WastageRecordDto::getProduct,
                r -> r.getProductId() != null ? r.getProductId().toString() : null,
                10, true);
        List<WastageGroupDto> outletBreakdown = groupBy(filtered,
                WastageRecordDto::getOutlet,
                r -> r.getOutletId() != null ? r.getOutletId().toString() : null,
                Integer.MAX_VALUE, true);
        List<WastageGroupDto> reasonBreakdown = groupBy(filtered,
                WastageRecordDto::getReason,
                WastageRecordDto::getReason,
                Integer.MAX_VALUE, false);
        List<WastageTrendPointDto> trend = buildTrend(filtered, startDate, endDate);

        boolean valueGap = !filtered.isEmpty()
                && filtered.stream().anyMatch(r -> r.getValue() == null);

        List<WastageRecordDto> sample = filtered.stream()
                .sorted(Comparator.comparing(WastageRecordDto::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(DEFAULT_RECORDS_LIMIT)
                .toList();

        WastageDashboardDto.WastageDashboardDtoBuilder builder = WastageDashboardDto.builder()
                .from(startDate)
                .to(endDate)
                .summary(summary)
                .topProducts(topProducts)
                .outletBreakdown(outletBreakdown)
                .reasonBreakdown(reasonBreakdown)
                .trend(trend)
                .records(sample);

        if (valueGap) {
            builder.dataLimitation("value-not-available")
                    .dataLimitationReason("Some wastage sources (notably outlet returns) do not yet expose a per-unit price; their value is excluded from totals.");
        }
        return builder.build();
    }

    @Override
    public List<WastageRecordDto> getRecords(LocalDate startDate,
                                             LocalDate endDate,
                                             Long outletId,
                                             Long productId,
                                             String reason,
                                             int page,
                                             int size) {
        validateDateRange(startDate, endDate);
        String canonicalReason = normaliseReason(reason);

        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);

        Lookups lookups = loadLookups();
        List<WastageRecordDto> filtered = filterRecords(
                collectAllRecords(lookups),
                startDate, endDate, outletId, productId, canonicalReason);
        filtered.sort(Comparator.comparing(WastageRecordDto::getDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int from = safePage * safeSize;
        if (from >= filtered.size()) return List.of();
        int to = Math.min(from + safeSize, filtered.size());
        return filtered.subList(from, to);
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

    /** Indexed lookup tables fetched once per request. */
    private record Lookups(Map<Long, Outlet> outlets,
                           Map<Long, Product> products,
                           Map<Long, ProductionCenter> productionCenters,
                           Map<Long, ProductionPlanItem> productionPlanItems) {}

    private Lookups loadLookups() {
        return new Lookups(
                indexById(outletRepository.findAll(), Outlet::getOutletId),
                indexById(productRepository.findAll(), Product::getId),
                indexById(productionCenterRepository.findAll(), ProductionCenter::getId),
                indexById(productionPlanItemRepository.findAll(), ProductionPlanItem::getId));
    }

    private List<WastageRecordDto> collectAllRecords(Lookups lookups) {
        List<WastageRecordDto> rows = new ArrayList<>();
        rows.addAll(productionWastageRecords(lookups));
        rows.addAll(posReturnRecords(lookups));
        rows.addAll(outletReturnRecords(lookups));
        rows.addAll(stockAdjustmentRecords());
        return rows;
    }

    /** Production-batch wastage. Reused conceptually from the existing
     *  Finance wastage report; logic mirrors that path but emits the new
     *  MIS-shaped DTO. */
    private List<WastageRecordDto> productionWastageRecords(Lookups lookups) {
        List<WastageRecordDto> rows = new ArrayList<>();
        for (ProductionBatch batch : productionBatchRepository.findAll()) {
            if (batch.getWastageQty() == null || batch.getWastageQty() <= 0) continue;

            ProductionPlanItem ppi = batch.getProductionPlanItemId() != null
                    ? lookups.productionPlanItems().get(batch.getProductionPlanItemId())
                    : null;
            Long productId = ppi != null ? ppi.getProductId() : null;
            Product product = productId != null ? lookups.products().get(productId) : null;

            String productName;
            BigDecimal unitPrice = null;
            if (product != null) {
                productName = product.getProductName();
                if (product.getUnitPrice() != null) {
                    unitPrice = BigDecimal.valueOf(product.getUnitPrice());
                }
            } else if (ppi != null) {
                productName = ppi.getProductName() != null
                        ? ppi.getProductName() : "PPI-" + ppi.getId();
                if (ppi.getUnitCost() != null) {
                    unitPrice = BigDecimal.valueOf(ppi.getUnitCost());
                }
            } else {
                productName = "PPI-" + batch.getProductionPlanItemId();
            }

            int qty = batch.getWastageQty();
            BigDecimal value = unitPrice != null
                    ? unitPrice.multiply(BigDecimal.valueOf(qty))
                            .setScale(2, RoundingMode.HALF_UP)
                    : null;

            ProductionCenter pc = batch.getProductionCenterId() != null
                    ? lookups.productionCenters().get(batch.getProductionCenterId())
                    : null;
            String outlet = pc != null ? pc.getCenterName()
                    : (batch.getProductionCenterId() != null
                        ? "Center #" + batch.getProductionCenterId() : "—");

            String rawReason = batch.getWastageReason() != null
                    ? batch.getWastageReason() : "Production wastage";
            String canonicalReason = mapFreeTextReason(batch.getWastageReason(), REASON_OTHER);

            rows.add(WastageRecordDto.builder()
                    .id("PRD-" + batch.getId())
                    .date(batch.getCreatedAt() != null ? batch.getCreatedAt().toLocalDate() : null)
                    .outlet(outlet)
                    .outletId(batch.getProductionCenterId())
                    .product(productName)
                    .productId(productId)
                    .qty(qty)
                    .unit("units")
                    .value(value)
                    .reason(canonicalReason)
                    .rawReason(rawReason)
                    .remarks(batch.getNotes())
                    .source(SOURCE_PRODUCTION)
                    .build());
        }
        return rows;
    }

    /** POS customer returns — wastage when a customer returns goods
     *  marked as not resellable (or when the return is for a damaged /
     *  expired item). We treat all return items as wastage records since
     *  the dashboard caller wants visibility into returned stock; the
     *  reason taxonomy puts unspecified ones into RETURNED. */
    private List<WastageRecordDto> posReturnRecords(Lookups lookups) {
        List<WastageRecordDto> rows = new ArrayList<>();
        for (Return ret : returnRepository.findAll()) {
            List<ReturnItem> items = ret.getReturnItems();
            if (items == null || items.isEmpty()) continue;

            String rawReason = ret.getReturnReason();
            String canonicalReason = mapFreeTextReason(rawReason, REASON_RETURNED);
            LocalDate date = ret.getCreatedAt() != null
                    ? ret.getCreatedAt().toLocalDate() : null;
            Long outletId = ret.getSale() != null ? ret.getSale().getOutletId() : null;
            String outletName = outletName(outletId, lookups.outlets());

            for (ReturnItem item : items) {
                if (item.getQty() == null || item.getQty() <= 0) continue;
                Long productId = item.getSaleItem() != null
                        ? item.getSaleItem().getProductId() : null;
                Product product = productId != null ? lookups.products().get(productId) : null;
                String productName = product != null ? product.getProductName()
                        : (productId != null ? "Product #" + productId : "Returned item");

                BigDecimal unitPrice = item.getUnitPrice();
                BigDecimal value = unitPrice != null
                        ? unitPrice.multiply(BigDecimal.valueOf(item.getQty()))
                                .setScale(2, RoundingMode.HALF_UP)
                        : null;

                rows.add(WastageRecordDto.builder()
                        .id("RTN-" + item.getReturnItemId())
                        .date(date)
                        .outlet(outletName)
                        .outletId(outletId)
                        .product(productName)
                        .productId(productId)
                        .qty(item.getQty())
                        .unit("units")
                        .value(value)
                        .reason(canonicalReason)
                        .rawReason(rawReason != null ? rawReason : "Customer return")
                        .remarks(Boolean.FALSE.equals(item.getIsResellable())
                                ? "Not resellable" : null)
                        .source(SOURCE_POS_RETURN)
                        .build());
            }
        }
        return rows;
    }

    /** Outlet → store returns — typically wastage when outlets ship
     *  damaged / expired stock back. No per-item price is captured today,
     *  so value is left null for these records. */
    private List<WastageRecordDto> outletReturnRecords(Lookups lookups) {
        List<WastageRecordDto> rows = new ArrayList<>();
        for (OutletReturn ret : outletReturnRepository.findAll()) {
            List<OutletReturnItem> items = ret.getItems();
            if (items == null || items.isEmpty()) continue;

            String rawReason = ret.getReason();
            String canonicalReason = mapFreeTextReason(rawReason, REASON_RETURNED);
            LocalDate date = ret.getCreatedAt() != null
                    ? ret.getCreatedAt().toLocalDate() : null;
            String outletName = outletName(ret.getOutletId(), lookups.outlets());

            for (OutletReturnItem item : items) {
                if (item.getQty() == null || item.getQty() <= 0) continue;
                Product product = item.getProductId() != null
                        ? lookups.products().get(item.getProductId()) : null;
                String productName = product != null ? product.getProductName()
                        : (item.getProductId() != null
                            ? "Product #" + item.getProductId() : "Outlet return item");

                BigDecimal value = null;
                if (product != null && product.getUnitPrice() != null) {
                    value = BigDecimal.valueOf(product.getUnitPrice())
                            .multiply(BigDecimal.valueOf(item.getQty()))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                rows.add(WastageRecordDto.builder()
                        .id("ORT-" + item.getId())
                        .date(date)
                        .outlet(outletName)
                        .outletId(ret.getOutletId())
                        .product(productName)
                        .productId(item.getProductId())
                        .qty(item.getQty())
                        .unit("units")
                        .value(value)
                        .reason(canonicalReason)
                        .rawReason(rawReason != null ? rawReason : "Outlet return")
                        .remarks(item.getBatchNote())
                        .source(SOURCE_OUTLET_RETURN)
                        .build());
            }
        }
        return rows;
    }

    /** Stock adjustments flagged as wastage causes (DAMAGE / EXPIRED). We
     *  ignore non-wastage adjustments such as COUNTING_ERROR. */
    private List<WastageRecordDto> stockAdjustmentRecords() {
        List<WastageRecordDto> rows = new ArrayList<>();
        for (StockAdjust adj : stockAdjustRepository.findAll()) {
            if (adj.getReasonForAdjust() == null) continue;
            String canonicalReason = mapStockAdjustReason(adj.getReasonForAdjust());
            // Only include adjustments that meaningfully represent wastage.
            if (canonicalReason.equals(REASON_OTHER)
                    && adj.getReasonForAdjust() != StockAdjustmentReason.OTHER) {
                continue;
            }

            Double change = adj.getChangeQuantity();
            if (change == null) continue;
            // Wastage adjustments are typically negative (-5kg) — treat
            // their absolute magnitude as wastage qty.
            int qty = (int) Math.round(Math.abs(change));
            if (qty <= 0) continue;

            BigDecimal value = null;
            String materialName = "Material #?";
            String unit = "units";
            if (adj.getRawMaterial() != null) {
                if (adj.getRawMaterial().getUnitCost() != null) {
                    value = BigDecimal.valueOf(adj.getRawMaterial().getUnitCost())
                            .multiply(BigDecimal.valueOf(qty))
                            .setScale(2, RoundingMode.HALF_UP);
                }
                if (adj.getRawMaterial().getMaterialName() != null) {
                    materialName = adj.getRawMaterial().getMaterialName();
                }
                if (adj.getRawMaterial().getUnitOfMeasure() != null) {
                    unit = adj.getRawMaterial().getUnitOfMeasure();
                }
            }

            rows.add(WastageRecordDto.builder()
                    .id("ADJ-" + adj.getId())
                    .date(adj.getCreatedAt() != null
                            ? adj.getCreatedAt().toLocalDate() : null)
                    .outlet("Warehouse")
                    .outletId(null)
                    .product(materialName)
                    .productId(null)
                    .qty(qty)
                    .unit(unit)
                    .value(value)
                    .reason(canonicalReason)
                    .rawReason(adj.getReasonForAdjust().getDisplayName())
                    .remarks(adj.getRemarks())
                    .source(SOURCE_STOCK_ADJUSTMENT)
                    .build());
        }
        return rows;
    }

    // ──────────────────────────────────────────────────────────────────
    // Filtering / aggregation
    // ──────────────────────────────────────────────────────────────────

    private List<WastageRecordDto> filterRecords(List<WastageRecordDto> all,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 Long outletId,
                                                 Long productId,
                                                 String reason) {
        List<WastageRecordDto> result = new ArrayList<>(all.size());
        for (WastageRecordDto r : all) {
            if (!withinRange(r.getDate(), startDate, endDate)) continue;
            if (outletId != null && !outletId.equals(r.getOutletId())) continue;
            if (productId != null && !productId.equals(r.getProductId())) continue;
            if (reason != null && !reason.equals(r.getReason())) continue;
            result.add(r);
        }
        return result;
    }

    private WastageSummaryDto buildSummary(List<WastageRecordDto> rows,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        long totalQty = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        boolean anyValue = false;
        for (WastageRecordDto r : rows) {
            if (r.getQty() != null) totalQty += r.getQty();
            if (r.getValue() != null) {
                totalValue = totalValue.add(r.getValue());
                anyValue = true;
            }
        }
        BigDecimal totalValueOrNull = anyValue ? totalValue : null;

        BigDecimal salesValue = computeSalesTotal(startDate, endDate);
        boolean salesAvailable = salesValue != null && salesValue.signum() > 0;
        BigDecimal wastagePercent = null;
        if (salesAvailable && totalValueOrNull != null) {
            wastagePercent = totalValueOrNull
                    .divide(salesValue, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return WastageSummaryDto.builder()
                .totalQty(totalQty)
                .totalValue(totalValueOrNull)
                .wastagePercent(wastagePercent)
                .recordCount(rows.size())
                .salesDataAvailable(salesAvailable)
                .salesValue(salesAvailable ? salesValue : null)
                .build();
    }

    /** Sum of {@link Sale#getFinalTotal()} across the same window so we can
     *  compute wastage% of sales. Falls back to {@code totalAmount} when
     *  finalTotal is missing. Returns {@code null} when there are no sales
     *  (so the caller can flag salesDataAvailable=false). */
    private BigDecimal computeSalesTotal(LocalDate startDate, LocalDate endDate) {
        List<Sale> sales;
        if (startDate != null && endDate != null) {
            sales = saleRepository.findBySaleDateBetween(startDate, endDate);
        } else {
            sales = saleRepository.findAll().stream()
                    .filter(s -> withinRange(s.getSaleDate(), startDate, endDate))
                    .toList();
        }
        if (sales.isEmpty()) return null;
        BigDecimal total = BigDecimal.ZERO;
        for (Sale s : sales) {
            BigDecimal sub = s.getFinalTotal();
            if (sub == null || sub.signum() == 0) sub = s.getTotalAmount();
            if (sub == null) continue;
            total = total.add(sub);
        }
        return total.signum() == 0 ? null : total;
    }

    private List<WastageGroupDto> groupBy(List<WastageRecordDto> rows,
                                          java.util.function.Function<WastageRecordDto, String> labelFn,
                                          java.util.function.Function<WastageRecordDto, String> keyFn,
                                          int limit,
                                          boolean orderByValue) {
        record Bucket(long qty, BigDecimal value, int count, String key) {}
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (WastageRecordDto r : rows) {
            String label = labelFn.apply(r);
            if (label == null) label = "—";
            Bucket existing = buckets.get(label);
            BigDecimal value = r.getValue() != null ? r.getValue() : BigDecimal.ZERO;
            if (existing == null) {
                buckets.put(label, new Bucket(
                        r.getQty() != null ? r.getQty() : 0,
                        value,
                        1,
                        keyFn.apply(r)));
            } else {
                buckets.put(label, new Bucket(
                        existing.qty() + (r.getQty() != null ? r.getQty() : 0),
                        existing.value().add(value),
                        existing.count() + 1,
                        existing.key() != null ? existing.key() : keyFn.apply(r)));
            }
        }

        List<WastageGroupDto> out = new ArrayList<>();
        buckets.forEach((label, b) -> out.add(WastageGroupDto.builder()
                .label(label)
                .key(b.key())
                .qty(b.qty())
                .value(b.value().signum() == 0 ? null : b.value())
                .recordCount(b.count())
                .build()));

        if (orderByValue) {
            out.sort(Comparator.<WastageGroupDto, BigDecimal>comparing(
                    g -> g.getValue() != null ? g.getValue() : BigDecimal.ZERO,
                    Comparator.reverseOrder())
                .thenComparing(g -> -g.getQty()));
        } else {
            out.sort(Comparator.comparingLong(WastageGroupDto::getQty).reversed());
        }
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    private List<WastageTrendPointDto> buildTrend(List<WastageRecordDto> rows,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {
        // Anchor the window: prefer the explicit endDate (or today) for the
        // last day, and either the explicit startDate or "endDate - 6 days"
        // for the first day. The series is the smaller of TREND_DAYS and the
        // window's actual span so we never project beyond the requested range.
        LocalDate anchorEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate desiredStart = anchorEnd.minusDays(TREND_DAYS - 1L);
        LocalDate anchorStart = startDate != null && startDate.isAfter(desiredStart)
                ? startDate : desiredStart;
        if (anchorStart.isAfter(anchorEnd)) {
            anchorStart = anchorEnd;
        }

        Map<LocalDate, BigDecimal> valueByDay = new HashMap<>();
        Map<LocalDate, Long> qtyByDay = new HashMap<>();
        for (WastageRecordDto r : rows) {
            if (r.getDate() == null) continue;
            if (r.getDate().isBefore(anchorStart) || r.getDate().isAfter(anchorEnd)) continue;
            valueByDay.merge(r.getDate(),
                    r.getValue() != null ? r.getValue() : BigDecimal.ZERO,
                    BigDecimal::add);
            qtyByDay.merge(r.getDate(),
                    r.getQty() != null ? r.getQty().longValue() : 0L,
                    Long::sum);
        }

        List<WastageTrendPointDto> series = new ArrayList<>();
        for (LocalDate day = anchorStart; !day.isAfter(anchorEnd); day = day.plusDays(1)) {
            series.add(WastageTrendPointDto.builder()
                    .date(day)
                    .label(day.format(TREND_LABEL_FMT))
                    .value(valueByDay.getOrDefault(day, BigDecimal.ZERO))
                    .qty(qtyByDay.getOrDefault(day, 0L))
                    .build());
        }
        return series;
    }

    // ──────────────────────────────────────────────────────────────────
    // Reason taxonomy
    // ──────────────────────────────────────────────────────────────────

    /** Public so the controller and tests can normalise user-supplied
     *  filter values consistently. */
    public static String normaliseReason(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("EXPIR")) return REASON_EXPIRED;
        if (upper.startsWith("DAMAG")) return REASON_DAMAGED;
        if (upper.startsWith("RETURN")) return REASON_RETURNED;
        if (upper.equals("OTHER")) return REASON_OTHER;
        // Allow exact canonical names too.
        if (upper.equals(REASON_EXPIRED) || upper.equals(REASON_DAMAGED)
                || upper.equals(REASON_RETURNED)) {
            return upper;
        }
        // Anything else is treated as OTHER.
        return REASON_OTHER;
    }

    /** Map a free-text wastage reason from a source module to one of the
     *  canonical buckets, falling back to {@code defaultBucket} when no
     *  keyword matches. */
    static String mapFreeTextReason(String raw, String defaultBucket) {
        if (raw == null || raw.isBlank()) return defaultBucket;
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("expir")) return REASON_EXPIRED;
        if (lower.contains("damag")) return REASON_DAMAGED;
        if (lower.contains("return")) return REASON_RETURNED;
        return defaultBucket;
    }

    static String mapStockAdjustReason(StockAdjustmentReason reason) {
        if (reason == null) return REASON_OTHER;
        return switch (reason) {
            case EXPIRED -> REASON_EXPIRED;
            case DAMAGE -> REASON_DAMAGED;
            case OTHER -> REASON_OTHER;
            default -> REASON_OTHER; // LOSS, COUNTING_ERROR — treat as OTHER
        };
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    private static <T, K> Map<K, T> indexById(List<T> items,
                                              java.util.function.Function<T, K> keyFn) {
        Map<K, T> map = new HashMap<>();
        for (T item : items) {
            K key = keyFn.apply(item);
            if (key != null) map.put(key, item);
        }
        return map;
    }

    private static boolean withinRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return from == null && to == null;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private static String outletName(Long outletId, Map<Long, Outlet> outletIndex) {
        if (outletId == null) return "—";
        Outlet outlet = outletIndex.get(outletId);
        return outlet != null && outlet.getName() != null
                ? outlet.getName() : "Outlet #" + outletId;
    }
}
