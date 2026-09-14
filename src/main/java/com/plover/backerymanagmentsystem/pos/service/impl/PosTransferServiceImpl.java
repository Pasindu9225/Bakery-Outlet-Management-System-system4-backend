package com.plover.backerymanagmentsystem.pos.service.impl;

import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistory;
import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistoryAction;
import com.plover.backerymanagmentsystem.manager.repository.ActualProductionHistoryRepository;
import com.plover.backerymanagmentsystem.pos.dto.OutletTransferResponseDto;
import com.plover.backerymanagmentsystem.pos.model.*;
import com.plover.backerymanagmentsystem.pos.repository.*;
import com.plover.backerymanagmentsystem.pos.service.PosTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosTransferServiceImpl implements PosTransferService {

    private final OutletTransferRequestRepository outletTransferRequestRepository;
    private final DayProductionRepository dayProductionRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final GtnRepository gtnRepository;
    private final ActualProductionHistoryRepository actualProductionHistoryRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.OutletRepository outletRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OutletTransferResponseDto> getPendingTransfers(Long outletId) {
        return outletTransferRequestRepository.findBySourceOutletIdAndStatusOrderByCreatedAtDesc(outletId, OutletTransferRequestStatus.PENDING)
                .stream()
                .map(req -> {
                    String sourceName = outletRepository.findById(req.getSourceOutletId())
                            .map(com.plover.backerymanagmentsystem.manager.model.Outlet::getName)
                            .orElse("Unknown Source");
                    String destName = outletRepository.findById(req.getDestinationOutletId())
                            .map(com.plover.backerymanagmentsystem.manager.model.Outlet::getName)
                            .orElse("Unknown Destination");

                    return OutletTransferResponseDto.builder()
                            .id(req.getId())
                            .sourceOutletId(req.getSourceOutletId())
                            .sourceOutletName(sourceName)
                            .destinationOutletId(req.getDestinationOutletId())
                            .destinationOutletName(destName)
                            .productId(req.getProduct().getId())
                            .productName(req.getProduct().getProductName())
                            .requestedQuantity(req.getRequestedQuantity())
                            .approvedQuantity(req.getApprovedQuantity())
                            .status(req.getStatus().name())
                            .createdAt(req.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveTransfer(Long requestId, Integer approvedQuantity, java.util.UUID approverId) {
        OutletTransferRequest request = outletTransferRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Transfer request not found: " + requestId));

        if (request.getStatus() != OutletTransferRequestStatus.PENDING) {
            throw new RuntimeException("Request is not PENDING");
        }

        // 1. Decrease stock at source outlet
        LocalDate today = LocalDate.now();
        List<DayProduction> dayProductions = dayProductionRepository.findByOrderedDateAndOutletIdAndIsActiveTrue(today, request.getSourceOutletId());
        
        boolean stockDeducted = false;
        for (DayProduction dp : dayProductions) {
            List<DayProductionItem> items = dayProductionItemRepository.findByProductionIdInWithProduct(List.of(dp.getProductionId()));
            for (DayProductionItem dpi : items) {
                if (dpi.getProduct().getId().equals(request.getProduct().getId())) {
                    if (dpi.getCurrentQty() >= approvedQuantity) {
                        dpi.setCurrentQty(dpi.getCurrentQty() - approvedQuantity);
                        dayProductionItemRepository.save(dpi);
                        stockDeducted = true;
                        break;
                    }
                }
            }
            if (stockDeducted) break;
        }

        if (!stockDeducted) {
            throw new RuntimeException("Insufficient stock in active DayProduction records for this product.");
        }

        // 2. Update request status
        request.setStatus(OutletTransferRequestStatus.APPROVED);
        request.setApprovedQuantity(approvedQuantity);
        outletTransferRequestRepository.save(request);

        // 3. Create GTN for destination outlet
        Gtn gtn = Gtn.builder()
                .date(LocalDateTime.now())
                .status(GtnStatus.NOT_RECEIVED)
                .source(GtnSource.OUTLET)
                .outletId(request.getDestinationOutletId())
                .addedBy(approverId)
                .build();
                
        gtn = gtnRepository.save(gtn);

        GtnItem gtnItem = GtnItem.builder()
                .gtn(gtn)
                .product(request.getProduct())
                .expectedQty((double) approvedQuantity)
                .receivedQty(0.0)
                .status(GtnStatus.NOT_RECEIVED)
                .expiryDate(LocalDateTime.now().plusDays(2))
                .entryStatus(EntryStatus.SYSTEM)
                .unit(ProductUnit.PIECES)
                .build();
        gtn.setGtnItems(new java.util.ArrayList<>(java.util.List.of(gtnItem)));
        gtnRepository.save(gtn);

        // 4. Save History
        com.plover.backerymanagmentsystem.manager.model.Outlet destOutlet = outletRepository.findById(request.getDestinationOutletId())
                .orElse(null);
        String destName = destOutlet != null ? destOutlet.getName() : "Unknown Outlet";

        ActualProductionHistory history = ActualProductionHistory.builder()
                .productId(request.getProduct().getId())
                .productName(request.getProduct().getProductName())
                .quantity(approvedQuantity)
                .actionType(ActualProductionHistoryAction.DISTRIBUTION_OUT)
                .referenceName("Transfer to " + destName)
                .build();
        actualProductionHistoryRepository.save(history);

        log.info("Approved transfer request {}: {} units of {}", requestId, approvedQuantity, request.getProduct().getProductName());
    }

    @Override
    @Transactional
    public void rejectTransfer(Long requestId) {
        OutletTransferRequest request = outletTransferRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Transfer request not found: " + requestId));
        request.setStatus(OutletTransferRequestStatus.REJECTED);
        outletTransferRequestRepository.save(request);
        log.info("Rejected transfer request {}", requestId);
    }
}
