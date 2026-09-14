package com.plover.backerymanagmentsystem.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.finance.dto.ReportEnvelopeDto;
import com.plover.backerymanagmentsystem.finance.dto.ReportRowDto;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.finance.service.FinancialReportService;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionBatch;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import com.plover.backerymanagmentsystem.manager.model.Recipe;
import com.plover.backerymanagmentsystem.manager.model.RecipeIngredient;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository;
import com.plover.backerymanagmentsystem.pos.model.PaymentCategory;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.model.SaleItem;
import com.plover.backerymanagmentsystem.pos.model.DayProduction;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosing;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosingItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterialRequirement;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialRequirementRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link FinancialReportService}.
 *
 * <p>Each report family is built from existing entities; reports requiring
 * data we don't yet track (profitability, variance) return an envelope with
 * {@code dataUnavailable=true} so the UI can show a friendly empty state
 * rather than crash. Lookup tables (suppliers, outlets, products, raw
 * materials, production centers) are fetched once per call and indexed in
 * memory to avoid the N+1 trap.</p>
 */
import com.plover.backerymanagmentsystem.store_keeper.model.IouRequest;
import com.plover.backerymanagmentsystem.store_keeper.model.IouRequestItem;
import com.plover.backerymanagmentsystem.store_keeper.model.IouStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.IouRequestRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinancialReportServiceImpl implements FinancialReportService {

    private final SupplierPaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final GrnRepository grnRepository;
    private final GrnItemRepository grnItemRepository;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final OutletRepository outletRepository;
    private final ProductionBatchRepository productionBatchRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final StockAdjustRepository stockAdjustRepository;
    private final BillOfMaterialRepository billOfMaterialRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final DayProductionRepository dayProductionRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final DayEndClosingRepository dayEndClosingRepository;
    private final DayEndClosingItemRepository dayEndClosingItemRepository;
    private final RawMaterialRequirementRepository rawMaterialRequirementRepository;
    private final IouRequestRepository iouRequestRepository;

    // ──────────────────────────────────────────────────────────────────────
    // Payments
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getPaymentSummary(String period, LocalDate from, LocalDate to) {
        log.info("Report: paymentSummary period={} from={} to={}", period, from, to);

        Map<Long, Supplier> supplierIndex = indexById(supplierRepository.findAll(),
                Supplier::getSupplierId);

        // Sum invoiced per supplier per period (all GRNs in the date range).
        Map<Long, BigDecimal> invoicedBySupplier = new HashMap<>();
        for (Grn grn : grnRepository.findAll()) {
            LocalDate grnDate = grn.getReceivedDate() != null
                    ? grn.getReceivedDate().toLocalDate() : null;
            if (!withinRange(grnDate, from, to)) continue;
            if (grn.getGrnStatus() == GrnStatus.CANCELLED) continue;
            invoicedBySupplier.merge(grn.getSupplierId(), nz(grn.getTotal()), BigDecimal::add);
        }

        List<ReportRowDto> rows = new ArrayList<>();
        BigDecimal totalInvoicedAll = BigDecimal.ZERO;
        BigDecimal totalPaidAll = BigDecimal.ZERO;
        BigDecimal totalOutstandingAll = BigDecimal.ZERO;

        for (SupplierPayment p : paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc()) {
            if (!withinRange(p.getPaymentDate(), from, to)) continue;

            Supplier sup = supplierIndex.get(p.getSupplierId());
            BigDecimal supplierInvoiced = invoicedBySupplier.getOrDefault(
                    p.getSupplierId(), BigDecimal.ZERO);
            BigDecimal paid = nz(p.getAmount());
            BigDecimal outstanding = supplierInvoiced.subtract(paid);
            if (outstanding.signum() < 0) outstanding = BigDecimal.ZERO;

            String status = computePaymentStatus(p, outstanding);

            rows.add(ReportRowDto.builder()
                    .id("PMT-" + p.getPaymentId())
                    .date(p.getPaymentDate() != null ? p.getPaymentDate().toString() : null)
                    .supplier(sup != null ? sup.getName() : "Supplier #" + p.getSupplierId())
                    .ref(p.getPaymentRef())
                    .totalInvoiced(supplierInvoiced)
                    .paid(paid)
                    .outstanding(outstanding)
                    .status(status)
                    .build());

            totalInvoicedAll = totalInvoicedAll.add(supplierInvoiced);
            totalPaidAll = totalPaidAll.add(paid);
            totalOutstandingAll = totalOutstandingAll.add(outstanding);
        }

        // Include IOU cash advances and settlements in Payment Summary report
        for (IouRequest iou : iouRequestRepository.findAll()) {
            if (iou.getStatus() == IouStatus.REJECTED || iou.getStatus() == IouStatus.PENDING) continue;
            LocalDate iouDate = iou.getRequestDate();
            if (!withinRange(iouDate, from, to)) continue;

            BigDecimal issued = BigDecimal.valueOf(iou.getIssuedAmount() != null ? iou.getIssuedAmount() : iou.getTotalEstimatedAmount());
            BigDecimal actual = iou.getTotalActualAmount() != null ? BigDecimal.valueOf(iou.getTotalActualAmount()) : issued;
            BigDecimal diff = iou.getDifferenceAmount() != null ? BigDecimal.valueOf(iou.getDifferenceAmount()) : BigDecimal.ZERO;
            BigDecimal outstanding = diff.signum() < 0 ? diff.abs() : BigDecimal.ZERO;

            String statusStr = (iou.getStatus() == IouStatus.CLOSED || iou.getStatus() == IouStatus.SETTLED) ? "Cleared" : "Issued";

            rows.add(ReportRowDto.builder()
                    .id("IOU-CASH-" + iou.getId())
                    .date(iouDate != null ? iouDate.toString() : null)
                    .supplier("IOU: " + (iou.getReceiverName() != null ? iou.getReceiverName() : "Storekeeper Cash"))
                    .ref(iou.getJustification() != null ? iou.getJustification() : "IOU Cash Advance")
                    .totalInvoiced(actual)
                    .paid(issued)
                    .outstanding(outstanding)
                    .status(statusStr)
                    .build());

            totalInvoicedAll = totalInvoicedAll.add(actual);
            totalPaidAll = totalPaidAll.add(issued);
            totalOutstandingAll = totalOutstandingAll.add(outstanding);
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("invoiced", totalInvoicedAll);
        totals.put("paid", totalPaidAll);
        totals.put("outstanding", totalOutstandingAll);

        return ReportEnvelopeDto.builder()
                .reportType("payments")
                .subType(period)
                .period(period)
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    /**
     * Map the persisted payment status to the UI bucket. We deliberately do
     * not flag overdue here: the FR-FIN-04 payments report is a payment-log
     * view, not the outstanding-summary view (which has its own overdue
     * computation in FR-FIN-02).
     */
    private String computePaymentStatus(SupplierPayment p, BigDecimal outstanding) {
        String raw = p.getStatus();
        if ("CLEARED".equalsIgnoreCase(raw)) return "Cleared";
        if (outstanding.signum() <= 0) return "Cleared";
        return "Pending";
    }

    // ──────────────────────────────────────────────────────────────────────
    // Sales
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getSalesReport(String type, LocalDate from, LocalDate to, Long outletId) {
        log.info("Report: sales type={} from={} to={} outletId={}", type, from, to, outletId);

        Map<Long, Outlet> outletIndex = indexById(outletRepository.findAll(), Outlet::getOutletId);

        List<Sale> sales = listSalesInRange(from, to);
        if (outletId != null) {
            sales = sales.stream()
                    .filter(s -> outletId.equals(s.getOutletId()))
                    .toList();
        }

        String normalisedType = normalizeSubType(type);

        List<ReportRowDto> rows;
        if ("byInterval".equals(normalisedType)) {
            rows = salesByInterval(sales, outletIndex);
        } else if ("discountsReturns".equals(normalisedType)) {
            rows = salesDiscountsReturns(sales, outletIndex);
        } else {
            // default and "byProduct"
            rows = salesByProduct(sales, outletIndex);
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("sales", sumDecimal(rows, ReportRowDto::getSales));
        totals.put("discounts", sumDecimal(rows, ReportRowDto::getDiscounts));
        totals.put("returns", sumDecimal(rows, ReportRowDto::getReturnsAmount));
        totals.put("net", sumDecimal(rows, ReportRowDto::getNet));

        return ReportEnvelopeDto.builder()
                .reportType("sales")
                .subType(normalisedType)
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    private List<ReportRowDto> salesByProduct(List<Sale> sales, Map<Long, Outlet> outletIndex) {
        // Pre-load all products into an index to avoid N+1 lazy-loading issues.
        // SaleItem.product is LAZY; calling item.getProduct() frequently returns null
        // when the Hibernate session does not eagerly initialize it from a batch query.
        Map<Long, Product> productIndex = indexById(productRepository.findAll(), Product::getId);

        // Aggregate by (productId, outletId) so each product+outlet combo is one row.
        record Key(Long productId, Long outletId, String displayDate) {}
        Map<Key, ReportRowDto> agg = new LinkedHashMap<>();
        int rowCounter = 0;

        for (Sale sale : sales) {
            if (sale.getSaleItems() == null) continue;
            for (SaleItem item : sale.getSaleItems()) {
                BigDecimal net = nz(item.getPrice());
                BigDecimal discount = nz(item.getAppliedDiscount());
                BigDecimal gross = net.add(discount);
                int qty = item.getQty() != null ? item.getQty() : 0;

                Key key = new Key(item.getProductId(), sale.getOutletId(),
                        sale.getSaleDate() != null ? sale.getSaleDate().toString() : "");

                ReportRowDto row = agg.get(key);
                if (row == null) {
                    // Use pre-loaded index first; fall back to lazy proxy if available.
                    Product product = item.getProductId() != null
                            ? productIndex.get(item.getProductId())
                            : item.getProduct();
                    String productName = product != null && product.getProductName() != null
                            ? product.getProductName()
                            : (item.getProductId() != null ? "Product #" + item.getProductId() : "Unknown");
                    String category = product != null && product.getCategory() != null
                            ? product.getCategory() : "—";
                    row = ReportRowDto.builder()
                            .id("S-" + (++rowCounter))
                            .date(key.displayDate())
                            .product(productName)
                            .category(category)
                            .outlet(outletName(sale.getOutletId(), outletIndex))
                            .sales(BigDecimal.ZERO)
                            .discounts(BigDecimal.ZERO)
                            .returnsAmount(BigDecimal.ZERO)
                            .net(BigDecimal.ZERO)
                            .totalCost(BigDecimal.ZERO)
                            .qty(0)
                            .build();
                    agg.put(key, row);
                }
                row.setSales(row.getSales().add(gross));
                row.setDiscounts(row.getDiscounts().add(discount));
                row.setNet(row.getNet().add(net));

                BigDecimal unitCost = item.getProductId() != null
                        ? calculateProductBomUnitCost(item.getProductId())
                        : BigDecimal.ZERO;
                if (unitCost.compareTo(BigDecimal.ZERO) == 0) {
                    // Fall back: use unit price from the pre-loaded product index
                    Product p = item.getProductId() != null ? productIndex.get(item.getProductId()) : null;
                    if (p == null) p = item.getProduct(); // last resort: lazy proxy
                    if (p != null && p.getUnitPrice() != null) {
                        unitCost = BigDecimal.valueOf(p.getUnitPrice());
                    }
                }
                BigDecimal itemCost = unitCost.multiply(BigDecimal.valueOf(qty));
                row.setTotalCost(row.getTotalCost().add(itemCost));
                row.setQty((row.getQty() != null ? row.getQty() : 0) + qty);
            }
        }
        return new ArrayList<>(agg.values());
    }

    private List<ReportRowDto> salesByInterval(List<Sale> sales, Map<Long, Outlet> outletIndex) {
        // Aggregate per day across all outlets (or the filtered outlet).
        Map<String, ReportRowDto> agg = new LinkedHashMap<>();
        int rowCounter = 0;

        for (Sale sale : sales) {
            String day = sale.getSaleDate() != null ? sale.getSaleDate().toString() : "—";
            ReportRowDto row = agg.get(day);
            if (row == null) {
                row = ReportRowDto.builder()
                        .id("SI-" + (++rowCounter))
                        .date(day)
                        .product("All Products")
                        .category("—")
                        .outlet(outletName(sale.getOutletId(), outletIndex))
                        .sales(BigDecimal.ZERO)
                        .discounts(BigDecimal.ZERO)
                        .returnsAmount(BigDecimal.ZERO)
                        .net(BigDecimal.ZERO)
                        .build();
                agg.put(day, row);
            }
            BigDecimal saleTotal = nz(sale.getTotalAmount());
            BigDecimal discount = nz(sale.getDiscountAmount());
            BigDecimal net = nz(sale.getFinalTotal());
            if (net.signum() == 0) net = saleTotal.subtract(discount);
            row.setSales(row.getSales().add(saleTotal));
            row.setDiscounts(row.getDiscounts().add(discount));
            row.setNet(row.getNet().add(net));
        }
        return new ArrayList<>(agg.values());
    }

    private List<ReportRowDto> salesDiscountsReturns(List<Sale> sales, Map<Long, Outlet> outletIndex) {
        List<ReportRowDto> rows = new ArrayList<>();
        int rowCounter = 0;
        for (Sale sale : sales) {
            BigDecimal discount = nz(sale.getDiscountAmount());
            if (discount.signum() <= 0) continue;
            BigDecimal saleTotal = nz(sale.getTotalAmount());
            BigDecimal net = nz(sale.getFinalTotal());
            if (net.signum() == 0) net = saleTotal.subtract(discount);
            rows.add(ReportRowDto.builder()
                    .id("DR-" + (++rowCounter))
                    .date(sale.getSaleDate() != null ? sale.getSaleDate().toString() : null)
                    .product("Sale #" + sale.getSaleId())
                    .category("—")
                    .outlet(outletName(sale.getOutletId(), outletIndex))
                    .sales(saleTotal)
                    .discounts(discount)
                    .returnsAmount(BigDecimal.ZERO)
                    .net(net)
                    .build());
        }
        return rows;
    }

    private List<Sale> listSalesInRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return saleRepository.findBySaleDateBetween(from, to);
        }
        // Fall back to all sales then filter — acceptable in the absence of a
        // half-open range query and consistent with the small dataset size of
        // this slice.
        return saleRepository.findAll().stream()
                .filter(s -> withinRange(s.getSaleDate(), from, to))
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Wastage / production cost
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getWastageReport(String type, LocalDate from, LocalDate to, Long outletId) {
        log.info("Report: wastage type={} from={} to={} outletId={}", type, from, to, outletId);
        String normalisedType = normalizeSubType(type);

        if ("rawMaterialCosts".equals(normalisedType)) {
            return rawMaterialCostsReport(from, to);
        }

        Map<Long, ProductionCenter> pcIndex = indexById(productionCenterRepository.findAll(),
                ProductionCenter::getId);
        Map<Long, ProductionPlanItem> ppiIndex = indexById(productionPlanItemRepository.findAll(),
                ProductionPlanItem::getId);
        Map<Long, Product> productIndex = indexById(productRepository.findAll(), Product::getId);
        Map<Long, Outlet> outletIndex = indexById(outletRepository.findAll(), Outlet::getOutletId);

        List<ReportRowDto> rows = new ArrayList<>();
        int rowCounter = 0;

        // Kitchen/Production Center Wastage
        for (ProductionBatch batch : productionBatchRepository.findAll()) {
            if (batch.getWastageQty() == null || batch.getWastageQty() <= 0) continue;
            LocalDate batchDate = batch.getCreatedAt() != null
                    ? batch.getCreatedAt().toLocalDate() : null;
            if (!withinRange(batchDate, from, to)) continue;
            if (outletId != null && !outletId.equals(batch.getProductionCenterId())) continue;

            ProductionPlanItem ppi = ppiIndex.get(batch.getProductionPlanItemId());
            String productName;
            String category = "—";
            BigDecimal costPerUnit = BigDecimal.ZERO;
            if (ppi != null) {
                Product product = ppi.getProductId() != null
                        ? productIndex.get(ppi.getProductId()) : null;
                if (product != null) {
                    productName = product.getProductName();
                    if (product.getCategory() != null) category = product.getCategory();
                    if (product.getUnitPrice() != null) {
                        costPerUnit = BigDecimal.valueOf(product.getUnitPrice());
                    }
                } else {
                    productName = ppi.getProductName() != null ? ppi.getProductName()
                            : "PPI-" + ppi.getId();
                    if (ppi.getUnitCost() != null) {
                        costPerUnit = BigDecimal.valueOf(ppi.getUnitCost());
                    }
                }
            } else {
                productName = "PPI-" + batch.getProductionPlanItemId();
            }

            int wastageQty = batch.getWastageQty();
            BigDecimal totalCost = costPerUnit.multiply(BigDecimal.valueOf(wastageQty));

            ProductionCenter pc = pcIndex.get(batch.getProductionCenterId());
            String outlet = pc != null ? pc.getCenterName()
                    : "Center #" + batch.getProductionCenterId();

            rows.add(ReportRowDto.builder()
                    .id("W-" + batch.getId())
                    .date(batchDate != null ? batchDate.toString() : null)
                    .product(productName)
                    .category(category)
                    .outlet(outlet)
                    .qty(wastageQty)
                    .unit("units")
                    .costPerUnit(costPerUnit)
                    .totalCost(totalCost)
                    .reason(batch.getWastageReason() != null ? batch.getWastageReason() : "Unspecified")
                    .build());
            rowCounter++;
        }

        // Outlet Day-End Closing Wastage
        for (DayEndClosing dec : dayEndClosingRepository.findAll()) {
            LocalDate decDate = dec.getClosingDate();
            if (!withinRange(decDate, from, to)) continue;
            if (outletId != null && !outletId.equals(dec.getOutletId())) continue;

            for (DayEndClosingItem item : dayEndClosingItemRepository.findByDayEndClosing_Id(dec.getId())) {
                if (item.getWastageQty() == null || item.getWastageQty() <= 0) continue;

                Product p = item.getProduct();
                String productName = p != null && p.getProductName() != null ? p.getProductName() : "Product #" + (p != null ? p.getId() : "?");
                String category = p != null && p.getCategory() != null ? p.getCategory() : "—";

                BigDecimal costPerUnit = BigDecimal.ZERO;
                if (p != null) {
                    costPerUnit = calculateProductBomUnitCost(p.getId());
                    if (costPerUnit.compareTo(BigDecimal.ZERO) == 0 && p.getUnitPrice() != null) {
                        costPerUnit = BigDecimal.valueOf(p.getUnitPrice());
                    }
                }

                int qty = item.getWastageQty();
                BigDecimal totalCost = costPerUnit.multiply(BigDecimal.valueOf(qty));
                String outletName = outletName(dec.getOutletId(), outletIndex);

                rows.add(ReportRowDto.builder()
                        .id("W-DEC-" + item.getId())
                        .date(decDate != null ? decDate.toString() : null)
                        .product(productName)
                        .category(category)
                        .outlet(outletName)
                        .qty(qty)
                        .unit("units")
                        .costPerUnit(costPerUnit)
                        .totalCost(totalCost)
                        .reason("Day End Closing Wastage")
                        .build());
                rowCounter++;
            }
        }

        // Outlet Daily Production Receipt Transport/Receive Wastage
        for (DayProduction dp : dayProductionRepository.findAll()) {
            LocalDate dpDate = dp.getOrderedDate();
            if (!withinRange(dpDate, from, to)) continue;
            if (outletId != null && !outletId.equals(dp.getOutletId())) continue;
            if (dp.getIsActive() != null && !dp.getIsActive()) continue;

            List<DayProductionItem> items = dayProductionItemRepository.findByProductionIdWithProduct(dp.getProductionId());
            for (DayProductionItem dpi : items) {
                if (dpi.getWastageQty() == null || dpi.getWastageQty() <= 0) continue;

                Product p = dpi.getProduct();
                String productName = p != null && p.getProductName() != null ? p.getProductName() : "Product #" + (p != null ? p.getId() : "?");
                String category = p != null && p.getCategory() != null ? p.getCategory() : "—";

                BigDecimal costPerUnit = BigDecimal.ZERO;
                if (p != null) {
                    costPerUnit = calculateProductBomUnitCost(p.getId());
                    if (costPerUnit.compareTo(BigDecimal.ZERO) == 0 && p.getUnitPrice() != null) {
                        costPerUnit = BigDecimal.valueOf(p.getUnitPrice());
                    }
                }

                int qty = dpi.getWastageQty();
                BigDecimal totalCost = costPerUnit.multiply(BigDecimal.valueOf(qty));
                String outletName = outletName(dp.getOutletId(), outletIndex);

                rows.add(ReportRowDto.builder()
                        .id("W-DP-" + dpi.getDayProductionItemId())
                        .date(dpDate != null ? dpDate.toString() : null)
                        .product(productName)
                        .category(category)
                        .outlet(outletName)
                        .qty(qty)
                        .unit("units")
                        .costPerUnit(costPerUnit)
                        .totalCost(totalCost)
                        .reason("Receive/Transfer Wastage")
                        .build());
                rowCounter++;
            }
        }

        // For "byOutlet" we sort wastage by outlet for the UI; the row shape
        // is the same.
        if ("byOutlet".equals(normalisedType)) {
            rows.sort(Comparator.comparing(
                    r -> r.getOutlet() != null ? r.getOutlet() : "",
                    String.CASE_INSENSITIVE_ORDER));
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", sumDecimal(rows, ReportRowDto::getTotalCost));
        totals.put("totalQty", BigDecimal.valueOf(rows.stream()
                .mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum()));

        return ReportEnvelopeDto.builder()
                .reportType("wastage")
                .subType(normalisedType)
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    private ReportEnvelopeDto rawMaterialCostsReport(LocalDate from, LocalDate to) {
        Map<Long, Supplier> supplierIndex = indexById(supplierRepository.findAll(),
                Supplier::getSupplierId);
        Map<Long, RawMaterial> rmIndex = indexById(rawMaterialRepository.findAll(),
                RawMaterial::getId);

        List<ReportRowDto> rows = new ArrayList<>();
        int rowCounter = 0;
        for (Grn grn : grnRepository.findAll()) {
            LocalDate grnDate = grn.getReceivedDate() != null
                    ? grn.getReceivedDate().toLocalDate() : null;
            if (!withinRange(grnDate, from, to)) continue;
            if (grn.getGrnStatus() == GrnStatus.CANCELLED) continue;

            Supplier sup = supplierIndex.get(grn.getSupplierId());
            for (GrnItem item : grnItemRepository.findByGrnId(grn.getGrnId())) {
                RawMaterial rm = rmIndex.get(item.getRawMaterialId());
                BigDecimal qty = item.getReceivedQuantity() != null
                        ? item.getReceivedQuantity() : BigDecimal.ZERO;
                BigDecimal price = nz(item.getPricePerUnit());
                BigDecimal totalCost = qty.multiply(price);
                rows.add(ReportRowDto.builder()
                        .id("RM-" + (++rowCounter))
                        .date(grnDate != null ? grnDate.toString() : null)
                        .product(rm != null ? rmDisplayName(rm) : "Material #" + item.getRawMaterialId())
                        .category(rm != null && rm.getCategory() != null ? rm.getCategory() : "—")
                        .supplier(sup != null ? sup.getName() : "Supplier #" + grn.getSupplierId())
                        .qty(qty.intValue())
                        .unit(item.getUom() != null ? item.getUom() : "")
                        .costPerUnit(price)
                        .totalCost(totalCost)
                        .reason("GRN-" + grn.getGrnId())
                        .build());
            }
        }

        // Include IOU raw material purchases in Raw Material Costs report
        for (IouRequest iou : iouRequestRepository.findAll()) {
            if (iou.getStatus() == IouStatus.REJECTED || iou.getStatus() == IouStatus.PENDING) continue;
            LocalDate iouDate = iou.getRequestDate();
            if (!withinRange(iouDate, from, to)) continue;

            if (iou.getItems() != null) {
                for (IouRequestItem item : iou.getItems()) {
                    Double qtyVal = item.getActualQuantity() != null ? item.getActualQuantity() : item.getEstimatedQuantity();
                    Double priceVal = item.getActualPrice() != null ? item.getActualPrice() : item.getEstimatedPrice();
                    if (qtyVal == null || qtyVal <= 0) continue;

                    BigDecimal qty = BigDecimal.valueOf(qtyVal);
                    BigDecimal price = BigDecimal.valueOf(priceVal != null ? priceVal : 0.0);
                    BigDecimal totalCost = qty.multiply(price);

                    String itemName = item.getActualItemName() != null && !item.getActualItemName().isBlank()
                            ? item.getActualItemName() : (item.getItemName() != null ? item.getItemName() : "IOU Material");

                    String supplier = item.getActualSupplierName() != null && !item.getActualSupplierName().isBlank()
                            ? item.getActualSupplierName() : (item.getSupplierName() != null ? item.getSupplierName() : "IOU Purchase");

                    rows.add(ReportRowDto.builder()
                            .id("RM-IOU-" + iou.getId() + "-" + (++rowCounter))
                            .date(iouDate != null ? iouDate.toString() : null)
                            .product(itemName)
                            .category("IOU Material")
                            .supplier(supplier)
                            .qty(qty.intValue())
                            .unit(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure() : "Unit")
                            .costPerUnit(price)
                            .totalCost(totalCost)
                            .reason("IOU-" + iou.getId() + " (" + (iou.getReceiverName() != null ? iou.getReceiverName() : "Storekeeper") + ")")
                            .build());
                }
            }
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", sumDecimal(rows, ReportRowDto::getTotalCost));

        return ReportEnvelopeDto.builder()
                .reportType("wastage")
                .subType("rawMaterialCosts")
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Profitability (data unavailable)
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getProfitabilityReport(String type, LocalDate from, LocalDate to) {
        log.info("Report: profitability type={} from={} to={}", type, from, to);
        return ReportEnvelopeDto.builder()
                .reportType("profitability")
                .subType(normalizeSubType(type))
                .from(from).to(to)
                .dataUnavailable(true)
                .unavailableReason("Profitability analysis needs product recipe and standard cost tracking which is not yet enabled.")
                .rows(List.of())
                .build();
    }


    // ──────────────────────────────────────────────────────────────────────
    // Staff meals
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getStaffMealReport(LocalDate from, LocalDate to) {
        log.info("Report: staffMeals from={} to={}", from, to);

        List<Sale> sales = listSalesInRange(from, to);

        List<ReportRowDto> rows = new ArrayList<>();
        BigDecimal totalCostAll = BigDecimal.ZERO;
        for (Sale sale : sales) {
            if (sale.getPaymentType() != PaymentCategory.FREE_MEAL) continue;
            String employee = "—";
            if (sale.getCashier() != null) {
                String first = nullToBlank(safeFirstName(sale.getCashier()));
                String last = nullToBlank(safeLastName(sale.getCashier()));
                String combined = (first + " " + last).trim();
                if (!combined.isBlank()) employee = combined;
            }

            String productList = "—";
            if (sale.getSaleItems() != null && !sale.getSaleItems().isEmpty()) {
                productList = sale.getSaleItems().stream()
                        .map(this::saleItemDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("—");
            }

            BigDecimal totalCost = nz(sale.getTotalAmount());
            totalCostAll = totalCostAll.add(totalCost);

            rows.add(ReportRowDto.builder()
                    .id("FM-" + sale.getSaleId())
                    .date(sale.getSaleDate() != null ? sale.getSaleDate().toString() : null)
                    .employee(employee)
                    .supplier(employee) // duplicate so older table renderers still pick it up
                    .product(productList)
                    .totalCost(totalCost)
                    .reason("Staff Meal")
                    .build());
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", totalCostAll);
        totals.put("count", BigDecimal.valueOf(rows.size()));

        return ReportEnvelopeDto.builder()
                .reportType("staffMeals")
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    private String saleItemDisplayName(SaleItem item) {
        if (item.getProduct() != null && item.getProduct().getProductName() != null) {
            return item.getProduct().getProductName();
        }
        if (item.getProductId() != null) return "Product #" + item.getProductId();
        return "Item #" + item.getSaleItemId();
    }

    /**
     * AuthModel uses camelCase field names ({@code firstName}/
     * {@code lastName}); their Lombok getters are {@code getFirstName()} /
     * {@code getLastName()}. We probe both naming conventions reflectively
     * so the report works even if the field names are renamed in the future.
     */
    private String safeFirstName(Object cashier) {
        String v = invokeStringGetter(cashier, "getFirstName");
        if (v != null) return v;
        return invokeStringGetter(cashier, "getFirst_name");
    }

    private String safeLastName(Object cashier) {
        String v = invokeStringGetter(cashier, "getLastName");
        if (v != null) return v;
        return invokeStringGetter(cashier, "getLast_name");
    }

    private String invokeStringGetter(Object target, String methodName) {
        if (target == null) return null;
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v != null ? v.toString() : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Stock movement
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getStockMovementReport(String type, LocalDate from, LocalDate to) {
        log.info("Report: stockMovement type={} from={} to={}", type, from, to);
        String normalisedType = normalizeSubType(type);

        if ("currentStock".equals(normalisedType)) {
            return currentStockReport();
        }
        if ("adjustments".equals(normalisedType)) {
            return adjustmentsReport(from, to);
        }
        if ("outflow".equals(normalisedType)) {
            return outflowReport(from, to);
        }
        // default + "inflow"
        return inflowReport(from, to);
    }

    private ReportEnvelopeDto inflowReport(LocalDate from, LocalDate to) {
        Map<Long, RawMaterial> rmIndex = indexById(rawMaterialRepository.findAll(),
                RawMaterial::getId);
        Map<Long, Supplier> supplierIndex = indexById(supplierRepository.findAll(),
                Supplier::getSupplierId);

        List<ReportRowDto> rows = new ArrayList<>();
        int rowCounter = 0;
        for (Grn grn : grnRepository.findAll()) {
            LocalDate grnDate = grn.getReceivedDate() != null
                    ? grn.getReceivedDate().toLocalDate() : null;
            if (!withinRange(grnDate, from, to)) continue;
            if (grn.getGrnStatus() == GrnStatus.CANCELLED) continue;

            Supplier sup = supplierIndex.get(grn.getSupplierId());
            for (GrnItem item : grnItemRepository.findByGrnId(grn.getGrnId())) {
                RawMaterial rm = rmIndex.get(item.getRawMaterialId());
                BigDecimal qty = item.getReceivedQuantity() != null
                        ? item.getReceivedQuantity() : BigDecimal.ZERO;
                BigDecimal price = nz(item.getPricePerUnit());
                rows.add(ReportRowDto.builder()
                        .id("IN-" + (++rowCounter))
                        .date(grnDate != null ? grnDate.toString() : null)
                        .product(rm != null ? rmDisplayName(rm) : "Material #" + item.getRawMaterialId())
                        .supplier(sup != null ? sup.getName() : "Supplier #" + grn.getSupplierId())
                        .qty(qty.intValue())
                        .unit(item.getUom() != null ? item.getUom() : "")
                        .costPerUnit(price)
                        .totalCost(qty.multiply(price))
                        .reason("Inflow")
                        .ref("GRN-" + grn.getGrnId())
                        .build());
            }
        }

        // Finished Goods Daily Dispatches (Internal receipts at POS outlets)
        for (DayProduction dp : dayProductionRepository.findAll()) {
            LocalDate dpDate = dp.getOrderedDate();
            if (!withinRange(dpDate, from, to)) continue;
            if (dp.getIsActive() != null && !dp.getIsActive()) continue;

            List<DayProductionItem> items = dayProductionItemRepository.findByProductionIdWithProduct(dp.getProductionId());
            for (DayProductionItem dpi : items) {
                BigDecimal qty = dpi.getReceivedQty() != null ? BigDecimal.valueOf(dpi.getReceivedQty()) : BigDecimal.ZERO;
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

                Product p = dpi.getProduct();
                String productName = p != null && p.getProductName() != null ? p.getProductName() : "Product #" + (p != null ? p.getId() : "?");

                BigDecimal price = BigDecimal.ZERO;
                if (p != null) {
                    price = calculateProductBomUnitCost(p.getId());
                    if (price.compareTo(BigDecimal.ZERO) == 0 && p.getUnitPrice() != null) {
                        price = BigDecimal.valueOf(p.getUnitPrice());
                    }
                }

                rows.add(ReportRowDto.builder()
                        .id("IN-DP-" + (++rowCounter))
                        .date(dpDate != null ? dpDate.toString() : null)
                        .product(productName)
                        .supplier("Internal Production")
                        .qty(qty.intValue())
                        .unit("units")
                        .costPerUnit(price)
                        .totalCost(qty.multiply(price))
                        .reason("Internal Receipt")
                        .ref("DP-" + dp.getProductionId())
                        .build());
            }
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", sumDecimal(rows, ReportRowDto::getTotalCost));
        return ReportEnvelopeDto.builder()
                .reportType("stockMovement")
                .subType("inflow")
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    private ReportEnvelopeDto outflowReport(LocalDate from, LocalDate to) {
        List<ReportRowDto> rows = new ArrayList<>();
        int rowCounter = 0;

        for (RawMaterialRequirement req : rawMaterialRequirementRepository.findAll()) {
            ProductionOrder order = req.getProductionOrder();
            if (order == null) continue;

            LocalDate reqDate = order.getOrderDate() != null 
                    ? order.getOrderDate().toLocalDate() 
                    : (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null);

            if (!withinRange(reqDate, from, to)) continue;

            RawMaterial rm = req.getRawMaterial();
            String materialName = rm != null ? rmDisplayName(rm) : "Material #" + (rm != null ? rm.getId() : "?");
            String uom = req.getUnitOfMeasure() != null ? req.getUnitOfMeasure() : (rm != null ? rm.getUnitOfMeasure() : "");

            BigDecimal qty = req.getRequiredQuantity() != null ? BigDecimal.valueOf(req.getRequiredQuantity()) : BigDecimal.ZERO;
            BigDecimal unitCost = req.getUnitCost() != null ? BigDecimal.valueOf(req.getUnitCost()) : BigDecimal.ZERO;
            BigDecimal totalCost = req.getTotalCost() != null ? BigDecimal.valueOf(req.getTotalCost()) : qty.multiply(unitCost);

            rows.add(ReportRowDto.builder()
                    .id("OUT-" + (++rowCounter))
                    .date(reqDate != null ? reqDate.toString() : null)
                    .product(materialName)
                    .qty(qty.intValue())
                    .unit(uom)
                    .costPerUnit(unitCost)
                    .totalCost(totalCost)
                    .reason("Production Issuance")
                    .ref(order.getOrderNumber() != null ? order.getOrderNumber() : "PO-" + order.getId())
                    .build());
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", sumDecimal(rows, ReportRowDto::getTotalCost));

        return ReportEnvelopeDto.builder()
                .reportType("stockMovement")
                .subType("outflow")
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    private ReportEnvelopeDto adjustmentsReport(LocalDate from, LocalDate to) {
        List<ReportRowDto> rows = new ArrayList<>();
        for (StockAdjust adj : stockAdjustRepository.findAll()) {
            LocalDate adjDate = adj.getCreatedAt() != null ? adj.getCreatedAt().toLocalDate() : null;
            if (!withinRange(adjDate, from, to)) continue;
            RawMaterial rm = adj.getRawMaterial();
            BigDecimal change = adj.getChangeQuantity() != null
                    ? BigDecimal.valueOf(adj.getChangeQuantity()) : BigDecimal.ZERO;
            BigDecimal unitCost = rm != null && rm.getUnitCost() != null
                    ? BigDecimal.valueOf(rm.getUnitCost()) : BigDecimal.ZERO;
            String reason = adj.getReasonForAdjust() != null
                    ? adj.getReasonForAdjust().toString() : "Adjustment";
            rows.add(ReportRowDto.builder()
                    .id("ADJ-" + adj.getId())
                    .date(adjDate != null ? adjDate.toString() : null)
                    .product(rm != null ? rmDisplayName(rm) : "Material #?")
                    .qty(change.intValue())
                    .unit(rm != null && rm.getUnitOfMeasure() != null ? rm.getUnitOfMeasure() : "")
                    .costPerUnit(unitCost)
                    .totalCost(change.abs().multiply(unitCost))
                    .reason(reason)
                    .status(adj.getStatus() != null ? adj.getStatus().toString() : "—")
                    .build());
        }
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", sumDecimal(rows, ReportRowDto::getTotalCost));
        return ReportEnvelopeDto.builder()
                .reportType("stockMovement")
                .subType("adjustments")
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    private ReportEnvelopeDto currentStockReport() {
        List<ReportRowDto> rows = new ArrayList<>();
        for (RawMaterial rm : rawMaterialRepository.findByIsActiveTrue()) {
            BigDecimal qty = rm.getCurrentStock() != null
                    ? BigDecimal.valueOf(rm.getCurrentStock()) : BigDecimal.ZERO;
            BigDecimal unitCost = rm.getUnitCost() != null
                    ? BigDecimal.valueOf(rm.getUnitCost()) : BigDecimal.ZERO;
            rows.add(ReportRowDto.builder()
                    .id("CS-" + rm.getId())
                    .date(LocalDate.now().toString())
                    .product(rmDisplayName(rm))
                    .category(rm.getCategory() != null ? rm.getCategory() : "—")
                    .qty(qty.intValue())
                    .unit(rm.getUnitOfMeasure() != null ? rm.getUnitOfMeasure() : "")
                    .costPerUnit(unitCost)
                    .totalCost(qty.multiply(unitCost))
                    .reason("On Hand")
                    .build());
        }
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalValue", sumDecimal(rows, ReportRowDto::getTotalCost));
        totals.put("itemCount", BigDecimal.valueOf(rows.size()));
        return ReportEnvelopeDto.builder()
                .reportType("stockMovement")
                .subType("currentStock")
                .rows(rows)
                .totals(totals)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Purchase price analysis
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getPurchasePriceReport(String type, LocalDate from, LocalDate to) {
        log.info("Report: purchasePrice type={} from={} to={}", type, from, to);
        String normalisedType = normalizeSubType(type);

        Map<Long, RawMaterial> rmIndex = indexById(rawMaterialRepository.findAll(),
                RawMaterial::getId);
        Map<Long, Supplier> supplierIndex = indexById(supplierRepository.findAll(),
                Supplier::getSupplierId);

        // Build a flat list of (date, supplierId, rawMaterialId, price, qty).
        record PriceEntry(LocalDate date, Long supplierId, Long rawMaterialId,
                          BigDecimal price, BigDecimal qty, String uom) {}
        List<PriceEntry> entries = new ArrayList<>();
        for (Grn grn : grnRepository.findAll()) {
            LocalDate grnDate = grn.getReceivedDate() != null
                    ? grn.getReceivedDate().toLocalDate() : null;
            if (!withinRange(grnDate, from, to)) continue;
            if (grn.getGrnStatus() == GrnStatus.CANCELLED) continue;
            for (GrnItem item : grnItemRepository.findByGrnId(grn.getGrnId())) {
                entries.add(new PriceEntry(grnDate, grn.getSupplierId(), item.getRawMaterialId(),
                        nz(item.getPricePerUnit()),
                        item.getReceivedQuantity() != null ? item.getReceivedQuantity() : BigDecimal.ZERO,
                        item.getUom() != null ? item.getUom() : ""));
            }
        }

        // Sort ascending by date so we can compute change vs previous price
        // for the same (supplier, material) pair.
        entries.sort(Comparator.comparing(
                (PriceEntry e) -> e.date() != null ? e.date() : LocalDate.MIN));

        Map<String, BigDecimal> previousPrice = new HashMap<>();
        List<ReportRowDto> rows = new ArrayList<>();
        int rowCounter = 0;
        for (PriceEntry e : entries) {
            String key = e.supplierId() + "|" + e.rawMaterialId();
            BigDecimal prev = previousPrice.get(key);
            BigDecimal change = null;
            if (prev != null && prev.signum() != 0) {
                change = e.price().subtract(prev)
                        .divide(prev, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            previousPrice.put(key, e.price());

            Supplier sup = e.supplierId() != null ? supplierIndex.get(e.supplierId()) : null;
            RawMaterial rm = e.rawMaterialId() != null ? rmIndex.get(e.rawMaterialId()) : null;
            rows.add(ReportRowDto.builder()
                    .id("PP-" + (++rowCounter))
                    .date(e.date() != null ? e.date().toString() : null)
                    .supplier(sup != null ? sup.getName() : "Supplier #" + e.supplierId())
                    .product(rm != null ? rmDisplayName(rm) : "Material #" + e.rawMaterialId())
                    .costPerUnit(e.price())
                    .qty(e.qty().intValue())
                    .unit(e.uom())
                    .totalCost(e.price().multiply(e.qty()))
                    .change(change)
                    .build());
        }

        // Sort the result based on subType.
        Comparator<ReportRowDto> sorter;
        switch (normalisedType) {
            case "byProduct":
                sorter = Comparator.comparing(
                        (ReportRowDto r) -> r.getProduct() != null ? r.getProduct() : "",
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case "priceChange":
                sorter = Comparator.comparing(
                        (ReportRowDto r) -> r.getChange() != null ? r.getChange().abs() : BigDecimal.ZERO,
                        Comparator.reverseOrder());
                break;
            default: // bySupplier
                sorter = Comparator.comparing(
                        (ReportRowDto r) -> r.getSupplier() != null ? r.getSupplier() : "",
                        String.CASE_INSENSITIVE_ORDER);
        }
        rows.sort(sorter);

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalCost", sumDecimal(rows, ReportRowDto::getTotalCost));
        return ReportEnvelopeDto.builder()
                .reportType("purchasePrice")
                .subType(normalisedType)
                .from(from).to(to)
                .rows(rows)
                .totals(totals)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Variance (data unavailable)
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public ReportEnvelopeDto getVarianceReport(String type, LocalDate from, LocalDate to) {
        log.info("Report: variance type={} from={} to={}", type, from, to);
        return ReportEnvelopeDto.builder()
                .reportType("variance")
                .subType(normalizeSubType(type))
                .from(from).to(to)
                .dataUnavailable(true)
                .unavailableReason("Variance reports (standard vs actual) require cost/quantity baselines not yet present in this slice.")
                .rows(List.of())
                .build();
    }


    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

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
        if (date == null) {
            return from == null && to == null;
        }
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private static String outletName(Long outletId, Map<Long, Outlet> outletIndex) {
        if (outletId == null) return "—";
        Outlet outlet = outletIndex.get(outletId);
        return outlet != null && outlet.getName() != null ? outlet.getName()
                : "Outlet #" + outletId;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String nullToBlank(String s) {
        return s != null ? s : "";
    }

    /**
     * Display name preference: brand name → material name → "Material #id".
     */
    private static String rmDisplayName(RawMaterial rm) {
        Optional<String> generic = Optional.ofNullable(rm.getGenericMaterialName())
                .filter(s -> !s.isBlank());
        if (generic.isPresent()) return generic.get();
        if (rm.getMaterialName() != null && !rm.getMaterialName().isBlank()) {
            return rm.getMaterialName();
        }
        return "Material #" + rm.getId();
    }

    private static BigDecimal sumDecimal(List<ReportRowDto> rows,
                                          java.util.function.Function<ReportRowDto, BigDecimal> getter) {
        return rows.stream()
                .map(getter)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Lower-camelCase the sub-type so the frontend can pass either
     * "By Product" or "byProduct" or "by-product" and we route consistently.
     *
     * <p>Inputs that are already camelCase (no separators, contains an
     * uppercase letter) are preserved verbatim with the first character
     * lower-cased — so {@code "byProduct"} stays {@code "byProduct"}.</p>
     */
    static String normalizeSubType(String type) {
        if (type == null || type.isBlank()) return "";
        String trimmed = type.trim();

        // Already a camelCase / camel-like single token? Preserve internal capitals.
        if (trimmed.matches("[A-Za-z][A-Za-z0-9]*") && containsLetter(trimmed)) {
            // Lower-case the first letter only.
            return Character.toLowerCase(trimmed.charAt(0)) + trimmed.substring(1);
        }

        // Otherwise: split on non-alphanumerics, lowercase, and camelify.
        String[] parts = trimmed.toLowerCase().split("[^a-z0-9]+");
        if (parts.length == 0) return "";
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private static boolean containsLetter(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) return true;
        }
        return false;
    }

    private BigDecimal calculateProductBomUnitCost(Long productId) {
        return calculateProductBomUnitCostHelper(productId, new java.util.HashSet<>());
    }

    private BigDecimal calculateProductBomUnitCostHelper(Long productId, java.util.Set<Long> visited) {
        if (productId == null || visited.contains(productId)) {
            return BigDecimal.ZERO;
        }
        visited.add(productId);

        List<BillOfMaterial> bomList = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(productId);
        if (bomList == null || bomList.isEmpty()) {
            visited.remove(productId);
            return BigDecimal.ZERO;
        }

        BigDecimal totalBomCost = BigDecimal.ZERO;
        for (BillOfMaterial bom : bomList) {
            BigDecimal qty = bom.getQuantity() != null ? bom.getQuantity() : BigDecimal.ZERO;
            if (bom.getChildType() == BillOfMaterial.ChildType.raw_material) {
                RawMaterial rm = resolveRawMaterial(bom.getChildItemId(), bom.getParentProductId());
                if (rm != null && rm.getUnitCost() != null) {
                    BigDecimal rawMaterialCost = BigDecimal.valueOf(rm.getUnitCost());
                    totalBomCost = totalBomCost.add(qty.multiply(rawMaterialCost));
                }
            } else if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                BigDecimal subProductCost = calculateProductBomUnitCostHelper(bom.getChildItemId(), visited);
                totalBomCost = totalBomCost.add(qty.multiply(subProductCost));
            }
        }
        visited.remove(productId);
        return totalBomCost;
    }

    private RawMaterial resolveRawMaterial(Long childItemId, Long parentProductId) {
        Recipe recipe = recipeRepository.findByProductIdAndIsActiveTrue(parentProductId).orElse(null);
        RawMaterial rm = null;
        final Long initialRawMaterialId = childItemId;
        boolean allowed = true;
        if (recipe != null && recipe.getRecipeIngredients() != null) {
            allowed = recipe.getRecipeIngredients().stream()
                    .anyMatch(ing -> ing.getRawMaterial() != null
                            && ing.getRawMaterial().getId() != null
                            && ing.getRawMaterial().getId().equals(initialRawMaterialId));
        }
        if (allowed) {
            rm = rawMaterialRepository.findById(initialRawMaterialId).orElse(null);
        }
        if (rm == null) {
            RecipeIngredient riById = recipeIngredientRepository.findById(childItemId).orElse(null);
            if (riById != null && riById.getRawMaterial() != null) {
                boolean belongsToRecipe = recipe == null || (riById.getRecipe() != null && riById.getRecipe().getId() != null
                        && riById.getRecipe().getId().equals(recipe.getId()));
                if (belongsToRecipe) {
                    rm = rawMaterialRepository.findById(riById.getRawMaterial().getId()).orElse(null);
                }
            }
        }
        return rm;
    }
}

