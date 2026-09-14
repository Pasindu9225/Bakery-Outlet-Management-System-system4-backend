package com.plover.backerymanagmentsystem.pos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.pos.dto.CreateGtnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateGtnResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetAllSalesResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetGtnProductsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetPartiallyReceivedItemsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetSaleByIdResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnReceiveRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnReceiveResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ManualEntryRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.ManualEntryResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.PaymentMethodResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.StandaloneKotRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.StandaloneKotResponseDto;
import com.plover.backerymanagmentsystem.pos.service.DayProductionService;
import com.plover.backerymanagmentsystem.pos.service.GtnService;
import com.plover.backerymanagmentsystem.pos.service.PaymentMethodService;
import com.plover.backerymanagmentsystem.pos.service.SaleService;
import com.plover.backerymanagmentsystem.pos.service.DayEndService;

import org.springframework.format.annotation.DateTimeFormat;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for POS (Point of Sale) operations
 */
@RestController
@RequestMapping("/api/pos/v1")
@RequiredArgsConstructor
@Slf4j
public class PosController {

    private final GtnService gtnService;
    private final DayProductionService dayProductionService;
    private final PaymentMethodService paymentMethodService;
    private final SaleService saleService;
    private final DayEndService dayEndService;
    private final com.plover.backerymanagmentsystem.pos.service.CashFloatService cashFloatService;

    /**
     * Get all GTN products that are not in 'received' or 'over received'
     * status.
     *
     * This endpoint returns: - GTN information (date, status, source, addedBy)
     * - GTN item information (expected qty, unit, expiry date) - Product names
     * for each item
     *
     * @return ResponseEntity containing the list of GTNs with product
     *         information
     */
    @GetMapping("/gtn/products")
    public ResponseEntity<GetGtnProductsResponseDto> getGtnProducts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long outletId) {
        log.info("Received request to get GTN products for outletId: {}", outletId);

        GetGtnProductsResponseDto response = gtnService.getGtnProducts(outletId);
        log.info("Successfully retrieved {} GTNs with products", response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    /**
     * Receive GTN items and update their status, create day production records
     *
     * @param requestDto The GTN receive request containing GTN ID and received
     *                   items
     * @return ResponseEntity containing the result of the operation
     */
    @PostMapping("/gtn/receive")
    public ResponseEntity<GtnReceiveResponseDto> receiveGtnItems(@RequestBody GtnReceiveRequestDto requestDto) {
        log.info("Received request to receive GTN items for GTN ID: {}", requestDto.getGtnId());

        GtnReceiveResponseDto response = gtnService.receiveGtnItems(requestDto);
        log.info("Successfully processed GTN receive for GTN ID: {}, Production ID: {}",
                response.getGtnId(), response.getProductionId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all partially received GTN items with full details
     *
     * @return ResponseEntity containing the list of partially received items
     */
    @GetMapping("/gtn/partially-received")
    public ResponseEntity<GetPartiallyReceivedItemsResponseDto> getPartiallyReceivedItems() {
        log.info("Received request to get partially received GTN items");

        GetPartiallyReceivedItemsResponseDto response = gtnService.getPartiallyReceivedItems();
        log.info("Successfully retrieved {} partially received items", response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a manual entry for a product
     *
     * @param requestDto The manual entry request containing product details,
     *                   quantity, unit, remarks, source, and user ID
     * @return ResponseEntity containing the result of the operation and details
     *         of the added product
     */
    @PostMapping("/manual-entry")
    public ResponseEntity<ManualEntryResponseDto> createManualEntry(@RequestBody ManualEntryRequestDto requestDto) {
        log.info("Received request to create manual entry for product ID: {}", requestDto.getProductId());

        ManualEntryResponseDto response = gtnService.createManualEntry(requestDto);
        log.info("Successfully created manual entry with GTN ID: {}, GTN Item ID: {}",
                response.getGtnId(), response.getGtnItemId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get today's production items with product details.
     *
     * This endpoint retrieves all production items for today's date, including:
     * - Product information (ID, name, code, unit price) - Production
     * quantities (ordered and received)
     *
     * @return ResponseEntity containing the list of today's production items
     */
    @GetMapping("/today-production-items")
    public ResponseEntity<List<DayProductionItemResponseDto>> getTodayProductionItems(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long outletId) {
        log.info("Received request to get today's production items for outletId: {}", outletId);

        try {
            List<DayProductionItemResponseDto> response = dayProductionService.getTodayProductionItems(outletId);
            log.info("Successfully retrieved {} production items for today", response.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching today's production items: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a new sale transaction
     *
     * This endpoint creates a new sale with multiple items. Each sale must: -
     * Have a valid cashier ID from the bmsauth table - All items must use the
     * same payment method - Day production items must exist - Free meals have
     * zero cost - Bank transfer code required for bank/online payments
     *
     * @param requestDto The sale creation request containing cashier ID and
     *                   items
     * @return ResponseEntity containing the created sale details
     */
    @PostMapping("/sales")
    public ResponseEntity<CreateSaleResponseDto> createSale(@Valid @RequestBody CreateSaleRequestDto requestDto) {
        log.info("Received request to create sale for cashier ID: {} with {} items",
                requestDto.getCashierId(), requestDto.getItems().size());

        try {
            CreateSaleResponseDto response = saleService.createSale(requestDto);
            log.info("Successfully created sale with ID: {}", response.getData().getSaleId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating sale: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all payment methods ordered by name.
     *
     * This endpoint retrieves all available payment methods from the
     * payment_methods table, sorted alphabetically by name.
     *
     * @return ResponseEntity containing the list of all payment methods
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodResponseDto>> getAllPaymentMethods() {
        log.info("Received request to get all payment methods");

        try {
            List<PaymentMethodResponseDto> response = paymentMethodService.getAllPaymentMethods();
            log.info("Successfully retrieved {} payment methods", response.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching payment methods: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all sales with summary information.
     *
     * This endpoint retrieves all sales from the database with summary
     * information including sale ID, date, time, cashier information, total
     * amount, and item count.
     *
     * @return ResponseEntity containing the list of all sales with summary data
     */
    @GetMapping("/sales")
    public ResponseEntity<GetAllSalesResponseDto> getAllSales() {
        log.info("Received request to get all sales");

        try {
            GetAllSalesResponseDto response = saleService.getAllSales();
            log.info("Successfully retrieved {} sales with total amount: {}",
                    response.getTotalCount(), response.getTotalSalesAmount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all sales: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get a specific sale by ID with complete details.
     *
     * This endpoint retrieves a specific sale by its ID, including complete
     * sale information and all associated sale items with product details,
     * payment information, and discount information.
     *
     * @param saleId the sale ID to retrieve
     * @return ResponseEntity containing the sale details with all items
     */
    @GetMapping("/sales/{saleId}")
    public ResponseEntity<GetSaleByIdResponseDto> getSaleById(@PathVariable Integer saleId) {
        log.info("Received request to get sale with ID: {}", saleId);

        try {
            GetSaleByIdResponseDto response = saleService.getSaleById(saleId);
            log.info("Successfully retrieved sale with ID: {} containing {} items",
                    saleId, response.getSaleItems().size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching sale with ID {}: {}", saleId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a new GTN (Goods Transfer Note) with its items.
     * This is a temporary endpoint to add data into the GTN tables.
     *
     * @param requestDto The GTN creation request
     * @return ResponseEntity containing the result of the operation
     */
    @PostMapping("/gtn/create")
    public ResponseEntity<CreateGtnResponseDto> createGtn(@RequestBody CreateGtnRequestDto requestDto) {
        log.info("Received request to create GTN from source: {}", requestDto.getSource());

        CreateGtnResponseDto response = gtnService.createGtn(requestDto);
        log.info("Successfully created GTN with ID: {}", response.getGtnId());

        return ResponseEntity.ok(response);
    }

    /**
     * Generate the Day-End reconciliation summary dynamically.
     */
    @GetMapping("/day-end/summary")
    public ResponseEntity<com.plover.backerymanagmentsystem.pos.dto.DayEndSummaryResponseDto> getDayEndSummary(
            @org.springframework.web.bind.annotation.RequestParam("closureDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate closureDate,
            @org.springframework.web.bind.annotation.RequestParam("cashierId") java.util.UUID cashierId) {
        log.info("Received request for Day-End summary for Cashier {} on {}", cashierId, closureDate);
        return ResponseEntity.ok(dayEndService.getDayEndSummary(closureDate, cashierId));
    }

    /**
     * Process Day-End closure.
     */
    @PostMapping("/day-end/close")
    public ResponseEntity<Map<String, Object>> processDayEnd(@Valid @RequestBody com.plover.backerymanagmentsystem.pos.dto.ShiftClosureRequestDto requestDto) {
        log.info("Received Day-End closure request for Cashier: {}", requestDto.getCashierId());
        dayEndService.processDayEnd(requestDto);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "Day successfully closed and locked.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Open the morning cash float.
     */
    @PostMapping("/cash-float/open")
    public ResponseEntity<Map<String, Object>> openCashFloat(@Valid @RequestBody com.plover.backerymanagmentsystem.pos.dto.OpenCashFloatRequestDto requestDto) {
        log.info("Received request to open cash float for cashier: {}", requestDto.getCashierId());
        cashFloatService.openFloat(requestDto);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "Opening float successfully declared.");
        return ResponseEntity.ok(response);
    }

    /**
     * Update the cash float (Manager only logic assumed).
     */
    @PutMapping("/cash-float/update")
    public ResponseEntity<Map<String, Object>> updateCashFloat(@Valid @RequestBody com.plover.backerymanagmentsystem.pos.dto.UpdateCashFloatRequestDto requestDto) {
        log.info("Received request to update cash float for cashier: {}", requestDto.getCashierId());
        cashFloatService.updateFloat(requestDto);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "Opening float updated successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * Get the status of the current day's float.
     */
    @GetMapping("/cash-float/status")
    public ResponseEntity<com.plover.backerymanagmentsystem.pos.dto.CashFloatStatusResponseDto> getCashFloatStatus(
            @org.springframework.web.bind.annotation.RequestParam("cashierId") java.util.UUID cashierId) {
        log.info("Checking cash float status for cashier: {}", cashierId);
        return ResponseEntity.ok(cashFloatService.getStatus(cashierId));
    }

    /**
     * Create a standalone KOT for a direct sale cart item (POSSales per-item KOT flow).
     */
    @PostMapping("/sales/standalone-kot")
    public ResponseEntity<StandaloneKotResponseDto> createStandaloneKot(
            @Valid @RequestBody StandaloneKotRequestDto dto) {
        log.info("Creating standalone KOT for dayProductionItemId={} qty={} pcId={}",
                dto.getDayProductionItemId(), dto.getQty(), dto.getProductionCenterId());
        return ResponseEntity.ok(saleService.createStandaloneKot(dto));
    }

    /**
     * Get real-time dashboard statistics for the POS module.
     * Includes today's sales, order counts, payment breakdowns, and stock alerts.
     *
     * @param outletId Optional outlet ID to filter statistics
     * @return ResponseEntity containing dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<com.plover.backerymanagmentsystem.pos.dto.PosDashboardStatsResponseDto> getDashboardStats(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long outletId) {
        log.info("Received request for POS dashboard stats for outletId: {}", outletId);
        return ResponseEntity.ok(saleService.getDashboardStats(outletId));
    }

    /**
     * Update the invoice printed status for a sale
     */
    @PutMapping("/sales/{saleId}/print-status")
    public ResponseEntity<Void> updateInvoicePrintedStatus(
            @PathVariable Integer saleId,
            @org.springframework.web.bind.annotation.RequestParam("printed") Boolean printed) {
        log.info("Updating invoice printed status for sale ID: {} to {}", saleId, printed);
        saleService.updateInvoicePrintedStatus(saleId, printed);
        return ResponseEntity.ok().build();
    }
}
