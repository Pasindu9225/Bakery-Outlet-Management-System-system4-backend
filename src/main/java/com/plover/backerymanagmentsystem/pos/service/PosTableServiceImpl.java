package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.pos.dto.CreatePosTableRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.AddTableItemRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.FinishTableBillingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.PosTableItemResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.PosTableResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.TableBillingDetailsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ProductionCenterResponseDto;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.PosTable;
import com.plover.backerymanagmentsystem.pos.model.PosTableItem;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.PosTableItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.PosTableRepository;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrderItem;
import com.plover.backerymanagmentsystem.store_keeper.repository.ProductionOrderRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.pos.exception.UserConfigurationException;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosTableServiceImpl implements PosTableService {

    private final PosTableRepository posTableRepository;
    private final PosTableItemRepository posTableItemRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final SaleService saleService;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final OutletProductionCenterRepository outletProductionCenterRepository;
    private final AuthRepository authRepository;

    @Override
    public List<PosTableResponseDto> getAllTables() {
        log.info("Fetching all tables from database");
        List<PosTable> tables = posTableRepository.findAll();
        return tables.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PosTableResponseDto createTable(CreatePosTableRequestDto requestDto) {
        log.info("Creating new table: {}", requestDto.getTableName());
        
        if (posTableRepository.findByTableNameIgnoreCase(requestDto.getTableName().trim()).isPresent()) {
            throw new IllegalArgumentException("A table with this name already exists");
        }
        
        PosTable table = PosTable.builder()
                .tableName(requestDto.getTableName().trim())
                .status(requestDto.getStatus())
                .seatCount(requestDto.getSeatCount())
                .build();
        
        PosTable savedTable = posTableRepository.save(table);
        log.info("Successfully created table with ID: {}", savedTable.getId());
        return mapToDto(savedTable);
    }

    @Override
    @Transactional
    public PosTableItemResponseDto addTableItem(AddTableItemRequestDto requestDto) {
        log.info("Adding item '{}' to table ID: {}", requestDto.getProductName(), requestDto.getTableId());

        // Update table status to 'Occupied' if not already
        PosTable table = posTableRepository.findById(requestDto.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + requestDto.getTableId()));
        
        if (!"Occupied".equalsIgnoreCase(table.getStatus())) {
            table.setStatus("Occupied");
            posTableRepository.save(table);
            log.info("Table ID: {} status updated to Occupied", requestDto.getTableId());
        }

        // Fetch the latest dayProductionItemId for the given product
        Integer dayProductionItemId = dayProductionItemRepository
                .findFirstByProduct_IdOrderByDayProductionItemIdDesc((long) requestDto.getProductId())
                .map(DayProductionItem::getDayProductionItemId)
                .orElseThrow(() -> new RuntimeException("No active production item found for product: " + requestDto.getProductName()));

        PosTableItem tableItem = PosTableItem.builder()
                .tableId(requestDto.getTableId())
                .productId(requestDto.getProductId())
                .productName(requestDto.getProductName())
                .qty(requestDto.getQty())
                .unitPrice(requestDto.getUnitPrice())
                .instructions(requestDto.getInstructions())
                .dayProductionItemId(dayProductionItemId)
                .isPaid(requestDto.getIsPaid())
                .build();

        PosTableItem savedItem = posTableItemRepository.save(tableItem);
        log.info("Successfully added item to table with ID: {}", savedItem.getId());

        return mapToDto(savedItem);
    }

    @Override
    public List<PosTableItemResponseDto> getUnpaidItemsByTable(Long tableId) {
        log.info("Fetching unpaid items for table ID: {}", tableId);
        List<PosTableItem> unpaidItems = posTableItemRepository.findAllByTableIdAndIsPaid(tableId, false);
        return unpaidItems.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TableBillingDetailsResponseDto getTableBillingDetails(Long tableId) {
        log.info("Fetching complete billing details for table ID: {}", tableId);
        
        // Get Table Info
        PosTable table = posTableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));
        
        // Get Unpaid Items
        List<PosTableItemResponseDto> unpaidItems = getUnpaidItemsByTable(tableId);
        
        // Calculate Total
        java.math.BigDecimal total = unpaidItems.stream()
                .map(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQty())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
        return TableBillingDetailsResponseDto.builder()
                .tableInfo(mapToDto(table))
                .unpaidItems(unpaidItems)
                .totalAmount(total)
                .build();
    }

    @Override
    @Transactional
    public CreateSaleResponseDto finishTableBilling(FinishTableBillingRequestDto requestDto) {
        log.info("Finishing billing for table ID: {}", requestDto.getTableId());

        // 0. Validate payment details (FR-POS-15)
        BigDecimal totalAmount = requestDto.getTotalAmount();
        if (requestDto.getAmountReceived().compareTo(totalAmount) < 0) {
            throw new RuntimeException("Payment received (" + requestDto.getAmountReceived() + 
                ") must be greater than or equal to total amount (" + totalAmount + ")");
        }

        BigDecimal changeAmount = requestDto.getAmountReceived().subtract(totalAmount);

        if (requestDto.getPaymentMethodId() == null) {
            throw new RuntimeException("Payment method selection is required.");
        }

        // 1. Sale the products using the logic from SaleService
        CreateSaleRequestDto saleRequest = CreateSaleRequestDto.builder()
                .cashierId(requestDto.getCashierId())
                .items(requestDto.getItems())
                .tableId(requestDto.getTableId())
                .skipKot(true)
                .globalPromotionId(requestDto.getGlobalPromotionId())
                .build();

        // Inject payment method into items if not already set (usually they all use the same per requirement)
        if (requestDto.getPaymentMethodId() != null) {
            saleRequest.getItems().forEach(item -> item.setPaymentMethodId(requestDto.getPaymentMethodId()));
        }

        CreateSaleResponseDto saleResponse = saleService.createSale(saleRequest);
        
        // Add payment details to response
        if (saleResponse.getData() != null) {
            saleResponse.getData().setReceivedAmount(requestDto.getAmountReceived());
            saleResponse.getData().setChangeAmount(changeAmount);
        }

        log.info("Successfully created sale for table billing. Sale ID: {}, Change: {}", 
                saleResponse.getData().getSaleId(), changeAmount);

        // 2. Update all unpaid items for this table as paid
        List<PosTableItem> unpaidItems = posTableItemRepository.findAllByTableIdAndIsPaid(requestDto.getTableId(), false);
        for (PosTableItem item : unpaidItems) {
            item.setIsPaid(true);
        }
        posTableItemRepository.saveAll(unpaidItems);
        log.info("Marked {} items for table ID: {} as paid", unpaidItems.size(), requestDto.getTableId());

        // 3. Update table status to 'Available'
        PosTable table = posTableRepository.findById(requestDto.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + requestDto.getTableId()));
        table.setStatus("Available");
        posTableRepository.save(table);
        log.info("Table ID: {} status updated to Available", requestDto.getTableId());

        return saleResponse;
    }

    @Override
    @Transactional
    public void transferTable(Long sourceTableId, Long targetTableId) {
        log.info("Transferring orders from table ID: {} to table ID: {}", sourceTableId, targetTableId);
        
        PosTable sourceTable = posTableRepository.findById(sourceTableId)
                .orElseThrow(() -> new RuntimeException("Source table not found"));
        PosTable targetTable = posTableRepository.findById(targetTableId)
                .orElseThrow(() -> new RuntimeException("Target table not found"));

        if ("Reserved".equalsIgnoreCase(targetTable.getStatus())) {
            throw new RuntimeException("Cannot transfer to a reserved table");
        }

        List<PosTableItem> unpaidItems = posTableItemRepository.findAllByTableIdAndIsPaid(sourceTableId, false);
        if (unpaidItems.isEmpty()) {
            throw new RuntimeException("No active items to transfer from source table");
        }

        for (PosTableItem item : unpaidItems) {
            item.setTableId(targetTableId);
        }
        posTableItemRepository.saveAll(unpaidItems);

        sourceTable.setStatus("Available");
        targetTable.setStatus("Occupied");
        posTableRepository.save(sourceTable);
        posTableRepository.save(targetTable);

        log.info("Successfully transferred {} items to table ID: {}", unpaidItems.size(), targetTableId);
    }

    @Override
    @Transactional
    public PosTableItemResponseDto generateKOT(Long tableItemId, Long productionCenterId) {
        log.info("Generating manual KOT for table item ID: {} for production center ID: {}", tableItemId, productionCenterId);

        Long currentOutletId = requireCurrentOutletId();
        OutletProductionCenter targetMpc = outletProductionCenterRepository.findById(productionCenterId)
                .orElseThrow(() -> new RuntimeException("Production center not found with id: " + productionCenterId));
        if (!targetMpc.getOutlet().getOutletId().equals(currentOutletId)) {
            throw new RuntimeException("Production center does not belong to your outlet");
        }

        PosTableItem item = posTableItemRepository.findById(tableItemId)
                .orElseThrow(() -> new RuntimeException("Table item not found"));
        
        if (item.getKotId() != null) {
            throw new RuntimeException("KOT already generated for this item");
        }

        DayProductionItem dpi = dayProductionItemRepository.findById(item.getDayProductionItemId())
                .orElseThrow(() -> new RuntimeException("Production item not found"));
        Product product = dpi.getProduct();

        String orderNumber = "KOT-MAN-" + System.currentTimeMillis() + "-" + productionCenterId;
        log.info("Generating KOT for item: {} with center: {}", product.getProductName(), productionCenterId);
        if (product.getUnitPrice() == null) {
            log.error("Product {} has no unit price", product.getProductName());
            throw new RuntimeException("Product " + product.getProductName() + " has no unit price. Cannot generate KOT.");
        }

        log.info("Creating ProductionOrder object for item: {} with order number: {}", product.getProductName(), orderNumber);
        ProductionOrder kot = ProductionOrder.builder()
                .orderNumber(orderNumber)
                .orderDate(LocalDateTime.now())
                .status(ProductionOrder.ProductionOrderStatus.PENDING)
                .build();
        
        log.info("Creating ProductionOrderItem object");
        ProductionOrderItem kotItem = ProductionOrderItem.builder()
                .productionOrder(kot)
                .productId(product.getId())
                .productName(product.getProductName())
                .plannedQuantity(item.getQty())
                .unitCost(product.getUnitPrice())
                .totalCost(product.getUnitPrice() * item.getQty())
                .productionCenterId(productionCenterId) 
                .build();
        
        kot.setProductionOrderItems(new ArrayList<>(Arrays.asList(kotItem)));
        log.info("Saving KOT to database...");
        ProductionOrder savedKot;
        try {
            savedKot = productionOrderRepository.save(kot);
        } catch (Exception e) {
            log.error("Failed to save ProductionOrder: {}", e.getMessage(), e);
            // Check for specific constraint violations if possible
            Throwable cause = e.getCause();
            while (cause != null) {
                log.error("Cause: {}", cause.getMessage());
                cause = cause.getCause();
            }
            throw new RuntimeException("Database error saving KOT. Check server logs for details.");
        }
        
        item.setKotId(savedKot.getId());
        posTableItemRepository.save(item);
        
        log.info("KOT generated successfully with ID: {}", savedKot.getId());
        return mapToDto(item);
    }

    @Override
    @Transactional
    public void cancelKOT(Long tableItemId, String verificationCode) {
        log.info("Cancelling KOT for table item ID: {} with verification code: {}", tableItemId, verificationCode);
        
        // Verify Manager
        authRepository.findByVerificationCode(verificationCode)
                .filter(u -> u.isActive() && "10".equals(u.getRoleId()))
                .orElseThrow(() -> new RuntimeException("Invalid or unauthorized manager verification code"));

        PosTableItem item = posTableItemRepository.findById(tableItemId)
                .orElseThrow(() -> new RuntimeException("Table item not found"));
        
        if (item.getKotId() != null) {
            productionOrderRepository.deleteById(item.getKotId());
            log.info("Deleted ProductionOrder with ID: {}", item.getKotId());
        }

        Long tableId = item.getTableId();
        posTableItemRepository.delete(item);
        log.info("Deleted PosTableItem with ID: {}", tableItemId);

        // Check if table is now empty
        List<PosTableItem> remainingItems = posTableItemRepository.findAllByTableIdAndIsPaid(tableId, false);
        if (remainingItems.isEmpty()) {
            PosTable table = posTableRepository.findById(tableId).orElse(null);
            if (table != null && "Occupied".equalsIgnoreCase(table.getStatus())) {
                table.setStatus("Available");
                posTableRepository.save(table);
            }
        }
    }

    @Override
    @Transactional
    public void updateTableStatus(Long tableId, String status) {
        log.info("Updating status for table ID: {} to {}", tableId, status);
        PosTable table = posTableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));
        table.setStatus(status);
        posTableRepository.save(table);
        log.info("Successfully updated status for table ID: {}", tableId);
    }

    @Override
    public List<ProductionCenterResponseDto> getAllProductionCenters() {
        Long outletId = requireCurrentOutletId();
        return outletProductionCenterRepository.findByOutlet_OutletIdAndIsActiveTrue(outletId).stream()
                .map(mpc -> ProductionCenterResponseDto.builder()
                        .id(mpc.getId())
                        .centerName(mpc.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PosTableItemResponseDto updateTableItem(Long tableItemId, Integer qty, String instructions) {
        log.info("Updating table item ID: {} with qty: {} and instructions: {}", tableItemId, qty, instructions);
        PosTableItem item = posTableItemRepository.findById(tableItemId)
                .orElseThrow(() -> new RuntimeException("Table item not found"));
        
        if (item.getKotId() != null) {
            throw new RuntimeException("Cannot update item after KOT is generated");
        }

        item.setQty(qty);
        item.setInstructions(instructions);
        PosTableItem saved = posTableItemRepository.save(item);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void removeTableItem(Long tableItemId) {
        log.info("Removing table item ID: {}", tableItemId);
        PosTableItem item = posTableItemRepository.findById(tableItemId)
                .orElseThrow(() -> new RuntimeException("Table item not found"));
        
        if (item.getKotId() != null) {
            throw new RuntimeException("Cannot remove item after KOT is generated");
        }

        posTableItemRepository.delete(item);
    }

    private PosTableResponseDto mapToDto(PosTable table) {
        return PosTableResponseDto.builder()
                .id(table.getId())
                .tableName(table.getTableName())
                .status(table.getStatus())
                .seatCount(table.getSeatCount())
                .build();
    }

    private PosTableItemResponseDto mapToDto(PosTableItem item) {
        return PosTableItemResponseDto.builder()
                .id(item.getId())
                .tableId(item.getTableId())
                .dayProductionItemId(item.getDayProductionItemId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .instructions(item.getInstructions())
                .isPaid(item.getIsPaid())
                .kotId(item.getKotId())
                .isKotEnabled(dayProductionItemRepository.findById(item.getDayProductionItemId())
                        .map(dpi -> dpi.getProduct().getIsKotEnabled())
                        .orElse(false))
                .build();
    }

    private Long requireCurrentOutletId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            log.error("requireCurrentOutletId: Unauthenticated request or invalid principal type. Auth: {}", auth);
            throw new UserConfigurationException("Unauthenticated request");
        }
        AuthModel principal = (AuthModel) auth.getPrincipal();
        Long outletId = principal.getOutletId();
        if (outletId == null) {
            String userId = IdUtil.bytesToUuidString(principal.getId());
            log.error("requireCurrentOutletId: User '{}' (ID: {}) is not assigned to an outlet", 
                principal.getUsername(), userId);
            throw new UserConfigurationException("User '" + principal.getUsername() + "' is not assigned to an outlet. Please contact administrator.");
        }
        return outletId;
    }
}
