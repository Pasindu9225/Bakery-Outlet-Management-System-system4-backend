package com.plover.backerymanagmentsystem.mis.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.mis.dto.MisSupplierDetailDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierListItemDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierPoGrnDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierSummaryDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierTransactionDto;
import com.plover.backerymanagmentsystem.mis.service.MisSupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing the FR-MIS-02 Supplier Overview endpoints.
 *
 * <p>Routes:</p>
 * <ul>
 *   <li>{@code GET /summary} — four metric cards.</li>
 *   <li>{@code GET ?search&status&startDate&endDate} — supplier list with
 *       server-side filters (sorting is client-side).</li>
 *   <li>{@code GET /{supplierId}} — full profile + counts for the modal.</li>
 *   <li>{@code GET /{supplierId}/transactions?search} — ledger rows.</li>
 *   <li>{@code GET /{supplierId}/po-grn} — joined PO/GRN listing.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/mis/suppliers")
@RequiredArgsConstructor
@Slf4j
public class MisSupplierController {

    private final MisSupplierService misSupplierService;

    @GetMapping("/summary")
    public ResponseEntity<MisSupplierSummaryDto> getSummary() {
        return ResponseEntity.ok(misSupplierService.getSummary());
    }

    @GetMapping
    public ResponseEntity<List<MisSupplierListItemDto>> listSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(misSupplierService.getSuppliers(
                search, status,
                parseDate("startDate", startDate),
                parseDate("endDate", endDate)));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<MisSupplierDetailDto> getSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(misSupplierService.getSupplierDetail(supplierId));
    }

    @GetMapping("/{supplierId}/transactions")
    public ResponseEntity<List<MisSupplierTransactionDto>> getSupplierTransactions(
            @PathVariable Long supplierId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(misSupplierService.getSupplierTransactions(supplierId, search));
    }

    @GetMapping("/{supplierId}/po-grn")
    public ResponseEntity<List<MisSupplierPoGrnDto>> getSupplierPoGrn(@PathVariable Long supplierId) {
        return ResponseEntity.ok(misSupplierService.getSupplierPoGrn(supplierId));
    }

    private LocalDate parseDate(String name, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid " + name + " '" + value + "'. Expected ISO-8601 (yyyy-MM-dd)");
        }
    }
}
