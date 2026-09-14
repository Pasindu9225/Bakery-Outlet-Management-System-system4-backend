package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReturnMaterialsRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialReturnItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnProcessingServiceImpl implements com.plover.backerymanagmentsystem.store_keeper.service.ReturnProcessingService {

    private final RawMaterialReturnItemRepository rawMaterialReturnItemRepository;
    private final RawMaterialRepository rawMaterialRepository;

    @Override
    @Transactional
    public void processReturn(ReturnMaterialsRequestDto request) {
        log.info("Processing return note {} with {} items", request.getReturnId(), request.getReturnItems().size());

        request.getReturnItems().forEach(item -> {
            // Update status to RETURNED if APPROVED
            if (item.getStatus() == ReturnStatus.APPROVED) {
                int updated = rawMaterialReturnItemRepository.bulkUpdateStatus(
                        request.getReturnId(), java.util.List.of(item.getReturnItemId()), ReturnStatus.RETURNED);
                log.info("Updated return item {} to RETURNED (updated={})", item.getReturnItemId(), updated);

                // Deduct stock on raw materials (find by id)
                RawMaterial material = rawMaterialRepository.findById(item.getRawMaterialId())
                        .orElseThrow(() -> new RuntimeException("Raw material not found: " + item.getRawMaterialId()));
                double previous = material.getCurrentStock() != null ? material.getCurrentStock() : 0d;
                double deduct = item.getReturnQuantity() != null ? item.getReturnQuantity() : 0d;
                double newStock = Math.max(0d, previous - deduct);
                material.setCurrentStock(newStock);
                rawMaterialRepository.save(material);
                log.info("Deducted stock for material {} from {} by {} => {}", material.getId(), previous, deduct, newStock);
            } else {
                log.info("Skipping item {} since status is not APPROVED: {}", item.getReturnItemId(), item.getStatus());
            }
        });
    }
}


