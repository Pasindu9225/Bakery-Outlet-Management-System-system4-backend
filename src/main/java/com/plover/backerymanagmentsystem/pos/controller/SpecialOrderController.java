package com.plover.backerymanagmentsystem.pos.controller;

import com.plover.backerymanagmentsystem.pos.dto.CreateSpecialOrderRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.SpecialOrderPaymentRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.SpecialOrderResponseDto;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.pos.service.SpecialOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for managing Special Orders (FR-POS-14)
 */
@RestController
@RequestMapping("/api/pos/v1/special-orders")
@RequiredArgsConstructor
public class SpecialOrderController {

    private final SpecialOrderService specialOrderService;

    @PostMapping
    public ResponseEntity<SpecialOrderResponseDto> initiateOrder(@Valid @RequestBody CreateSpecialOrderRequestDto requestDto) {
        return ResponseEntity.ok(specialOrderService.initiateOrder(requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialOrderResponseDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(specialOrderService.getOrderById(id));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SpecialOrderResponseDto>> getPendingOrders() {
        return ResponseEntity.ok(specialOrderService.getPendingOrders());
    }

    @GetMapping("/delivery-date/{date}")
    public ResponseEntity<List<SpecialOrderResponseDto>> getOrdersByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(specialOrderService.getOrdersByDeliveryDate(date));
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<SpecialOrderResponseDto> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody SpecialOrderPaymentRequestDto requestDto) {
        return ResponseEntity.ok(specialOrderService.recordPayment(id, requestDto));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<SpecialOrderResponseDto> approveAndClose(
            @PathVariable Long id,
            @RequestParam UUID managerId) {
        return ResponseEntity.ok(specialOrderService.approveAndCloseOrder(id, managerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SpecialOrderResponseDto> cancelOrder(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(specialOrderService.cancelOrder(id, reason));
    }

    @GetMapping("/verify-manager/{code}")
    public ResponseEntity<AuthModel> verifyManager(@PathVariable String code) {
        return ResponseEntity.ok(specialOrderService.verifyManagerCode(code));
    }
}
