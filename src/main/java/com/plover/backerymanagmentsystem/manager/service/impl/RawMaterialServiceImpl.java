package com.plover.backerymanagmentsystem.manager.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.dto.RawMaterialSummaryDto;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.service.RawMaterialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RawMaterialServiceImpl implements RawMaterialService {

    private final RawMaterialRepository rawMaterialRepository;

    @Override
    public List<RawMaterialSummaryDto> getAllRawMaterials() {
        log.info("Fetching all active raw materials for manager view");
        List<RawMaterial> rawMaterials = rawMaterialRepository.findAllByOrderByIdDesc();
        return rawMaterials.stream()
                .map(this::toDto)
                .toList();
    }

    private RawMaterialSummaryDto toDto(RawMaterial rawMaterial) {
        return RawMaterialSummaryDto.builder()
                .id(rawMaterial.getId())
                .materialName(rawMaterial.getMaterialName())
                .materialCode(rawMaterial.getMaterialCode())
                .currentStock(rawMaterial.getCurrentStock())
                .category(rawMaterial.getCategory())
                .unitOfMeasure(rawMaterial.getUnitOfMeasure())
                .unitCost(rawMaterial.getUnitCost())
                .build();
    }
}
