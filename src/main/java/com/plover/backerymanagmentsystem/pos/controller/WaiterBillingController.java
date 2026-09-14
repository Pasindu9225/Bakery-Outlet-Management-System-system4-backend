package com.plover.backerymanagmentsystem.pos.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.waiter.*;
import com.plover.backerymanagmentsystem.pos.service.WaiterBillingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/pos/v1/waiter-billing")
@RequiredArgsConstructor
@Slf4j
public class WaiterBillingController {

    private final WaiterBillingService waiterBillingService;

    @GetMapping("/waiters")
    public ResponseEntity<List<Map<String, Object>>> getWaitersForOutlet(@RequestParam Long outletId) {
        List<BmsAuth> waiters = waiterBillingService.getWaitersForOutlet(outletId);
        List<Map<String, Object>> response = waiters.stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", IdUtil.bytesToUuidString(w.getId()));
            map.put("firstName", w.getFirstName());
            map.put("lastName", w.getLastName());
            map.put("username", w.getUsername());
            map.put("waiterId", w.getWaiterId());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add-item")
    public ResponseEntity<?> addWaiterItem(@RequestBody AddWaiterItemRequestDto requestDto) {
        try {
            return ResponseEntity.ok(waiterBillingService.addWaiterItem(requestDto));
        } catch (Exception e) {
            log.error("Error in addWaiterItem: ", e);
            e.printStackTrace();
            // Return stacktrace for easy debugging
            return ResponseEntity.status(500).body(e.getMessage() + "\n" + 
                java.util.Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n")));
        }
    }

    @GetMapping("/waiter-details/{waiterId}")
    public ResponseEntity<WaiterBillingDetailsResponseDto> getWaiterBillingDetails(@PathVariable String waiterId) {
        return ResponseEntity.ok(waiterBillingService.getWaiterBillingDetails(waiterId));
    }

    @PostMapping("/finish-billing")
    public ResponseEntity<CreateSaleResponseDto> finishWaiterBilling(@RequestBody FinishWaiterBillingRequestDto requestDto) {
        return ResponseEntity.ok(waiterBillingService.finishWaiterBilling(requestDto));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeWaiterItem(@PathVariable Long itemId) {
        waiterBillingService.removeWaiterItem(itemId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferWaiterItems(@RequestParam String fromWaiterId, @RequestParam String toWaiterId) {
        waiterBillingService.transferWaiterItems(fromWaiterId, toWaiterId);
        return ResponseEntity.ok().build();
    }
}
