package com.plover.backerymanagmentsystem.manager.controller;

import com.plover.backerymanagmentsystem.manager.model.SemiFinishedBatchInventory;
import com.plover.backerymanagmentsystem.manager.service.SemiFinishedReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/semi-finished-batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SemiFinishedBatchInventoryController {

    private final SemiFinishedReservationService semiFinishedReservationService;

    @GetMapping("/ministore/{miniStoreId}")
    public ResponseEntity<List<Map<String, Object>>> getBatchesByMiniStore(@PathVariable Long miniStoreId) {
        List<SemiFinishedBatchInventory> batches = semiFinishedReservationService.getBatchInventoryForMiniStore(miniStoreId);
        
        List<Map<String, Object>> response = batches.stream().map(batch -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", batch.getId());
            map.put("productId", batch.getProductId());
            map.put("productName", batch.getProductName());
            map.put("batchNumber", batch.getBatchNumber());
            map.put("miniStoreId", batch.getMiniStoreId());
            map.put("initialQty", batch.getInitialQty());
            map.put("availableQty", batch.getAvailableQty());
            map.put("reservedQty", batch.getReservedQty());
            map.put("unreservedQty", batch.getUnreservedQty());
            map.put("unitOfMeasure", batch.getUnitOfMeasure());
            map.put("manufacturedDate", batch.getManufacturedDate());
            map.put("expiryDate", batch.getExpiryDate());
            map.put("isExpired", batch.isExpired());
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}/available")
    public ResponseEntity<Map<String, Object>> getAvailableStock(
            @PathVariable Long productId,
            @RequestParam(required = false) Long miniStoreId) {
        Double availableUnreserved = semiFinishedReservationService.getAvailableUnreservedStock(productId, miniStoreId);
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("miniStoreId", miniStoreId);
        response.put("availableUnreservedStock", availableUnreserved);
        return ResponseEntity.ok(response);
    }
}
