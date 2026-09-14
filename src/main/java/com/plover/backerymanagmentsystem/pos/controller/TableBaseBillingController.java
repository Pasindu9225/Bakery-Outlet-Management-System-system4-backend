package com.plover.backerymanagmentsystem.pos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.pos.dto.AddTableItemRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreatePosTableRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.FinishTableBillingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.PosTableItemResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.PosTableResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.TableBillingDetailsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ProductionCenterResponseDto;
import com.plover.backerymanagmentsystem.pos.service.PosTableService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/pos/v1/table-billing")
@RequiredArgsConstructor
@Slf4j
public class TableBaseBillingController {

    private final PosTableService posTableService;

    /**
     * Get all tables available in the database
     *
     * @return ResponseEntity containing the list of all tables
     */
    @GetMapping("/tables")
    public ResponseEntity<List<PosTableResponseDto>> getAllTables() {
        log.info("Received request to get all tables");
        List<PosTableResponseDto> response = posTableService.getAllTables();
        log.info("Successfully retrieved {} tables", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new table in the database
     *
     * @param requestDto The table creation request
     * @return ResponseEntity containing the created table details
     */
    @PostMapping("/tables")
    public ResponseEntity<PosTableResponseDto> createTable(@RequestBody CreatePosTableRequestDto requestDto) {
        log.info("Received request to create a new table: {}", requestDto.getTableName());
        PosTableResponseDto response = posTableService.createTable(requestDto);
        log.info("Successfully created table with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Add an item to a table
     *
     * @param requestDto The item details to add to the table
     * @return ResponseEntity containing the added item details
     */
    @PostMapping("/add-item")
    public ResponseEntity<PosTableItemResponseDto> addTableItem(@RequestBody AddTableItemRequestDto requestDto) {
        log.info("Received request to add item '{}' to table ID: {}", 
                requestDto.getProductName(), requestDto.getTableId());
        PosTableItemResponseDto response = posTableService.addTableItem(requestDto);
        log.info("Successfully added item with ID: {} to table ID: {}", 
                response.getId(), response.getTableId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all details related to a table including unpaid items
     *
     * @param tableId The ID of the table to retrieve details for
     * @return ResponseEntity containing table info and unpaid items
     */
    @GetMapping("/table-details/{tableId}")
    public ResponseEntity<TableBillingDetailsResponseDto> getTableBillingDetails(@PathVariable Long tableId) {
        log.info("Received request to get billing details for table ID: {}", tableId);
        TableBillingDetailsResponseDto response = posTableService.getTableBillingDetails(tableId);
        log.info("Successfully retrieved details for table ID: {} with {} unpaid items", 
                tableId, response.getUnpaidItems().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Finish the table billing process by creating a sale and marking items as paid
     *
     * @param requestDto The billing information including table ID and sale items
     * @return ResponseEntity containing the created sale details
     */
    @PostMapping("/finish-billing")
    public ResponseEntity<CreateSaleResponseDto> finishTableBilling(@RequestBody FinishTableBillingRequestDto requestDto) {
        log.info("Received request to finish billing for table ID: {}", requestDto.getTableId());
        CreateSaleResponseDto response = posTableService.finishTableBilling(requestDto);
        log.info("Successfully finished billing for table ID: {}. Sale ID: {}", 
                requestDto.getTableId(), response.getData().getSaleId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferTable(
            @RequestParam Long sourceTableId, 
            @RequestParam Long targetTableId) {
        log.info("Received request to transfer items from table ID: {} to {}", sourceTableId, targetTableId);
        posTableService.transferTable(sourceTableId, targetTableId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-kot/{tableItemId}")
    public ResponseEntity<PosTableItemResponseDto> generateKOT(
            @PathVariable Long tableItemId, 
            @RequestParam Long productionCenterId) {
        log.info("Received request to generate KOT for table item ID: {} for center ID: {}", 
                tableItemId, productionCenterId);
        PosTableItemResponseDto response = posTableService.generateKOT(tableItemId, productionCenterId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cancel-kot/{tableItemId}")
    public ResponseEntity<Void> cancelKOT(
            @PathVariable Long tableItemId, 
            @RequestParam String verificationCode) {
        log.info("Received request to cancel KOT for table item ID: {}", tableItemId);
        posTableService.cancelKOT(tableItemId, verificationCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/production-centers")
    public ResponseEntity<List<ProductionCenterResponseDto>> getAllProductionCenters() {
        log.info("Received request to get all production centers for POS");
        return ResponseEntity.ok(posTableService.getAllProductionCenters());
    }

    @PutMapping("/tables/{tableId}/status")
    public ResponseEntity<Void> updateTableStatus(
            @PathVariable Long tableId, 
            @RequestParam String status) {
        log.info("Received request to update status for table ID: {} to {}", tableId, status);
        posTableService.updateTableStatus(tableId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{tableItemId}")
    public ResponseEntity<PosTableItemResponseDto> updateTableItem(
            @PathVariable Long tableItemId, 
            @RequestParam Integer qty, 
            @RequestParam(required = false) String instructions) {
        log.info("Received request to update table item ID: {} with qty: {}", tableItemId, qty);
        PosTableItemResponseDto response = posTableService.updateTableItem(tableItemId, qty, instructions);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{tableItemId}")
    public ResponseEntity<Void> removeTableItem(@PathVariable Long tableItemId) {
        log.info("Received request to remove table item ID: {}", tableItemId);
        posTableService.removeTableItem(tableItemId);
        return ResponseEntity.ok().build();
    }
}
