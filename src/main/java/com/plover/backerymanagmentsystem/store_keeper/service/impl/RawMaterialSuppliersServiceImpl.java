package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialSuppliersResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialSuppliersResponseDto.SupplierInfoDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.NoSuppliersFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialSuppliersService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of RawMaterialSuppliersService. Handles supplier retrieval
 * operations with proper business logic and error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RawMaterialSuppliersServiceImpl implements RawMaterialSuppliersService {

    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;

    @Override
    public RawMaterialSuppliersResponseDto getSuppliersByRawMaterial(Long rawMaterialId) {
        log.info("Retrieving suppliers for raw material ID: {}", rawMaterialId);

        // Step 1: Validate that raw material exists
        Optional<RawMaterial> rawMaterialOpt = rawMaterialRepository.findById(rawMaterialId);
        if (rawMaterialOpt.isEmpty()) {
            log.error("Raw material not found with ID: {}", rawMaterialId);
            throw new RawMaterialNotFoundException(rawMaterialId);
        }

        RawMaterial rawMaterial = rawMaterialOpt.get();
        log.debug("Found raw material: {} (ID: {})", rawMaterial.getMaterialName(), rawMaterialId);

        // Step 2: Get suppliers for the raw material
        List<RawMaterialSupplier> rawMaterialSuppliers = rawMaterialSupplierRepository
                .findByRawMaterialIdWithDetails(rawMaterialId);

        if (rawMaterialSuppliers.isEmpty()) {
            log.warn("No suppliers found for raw material: {} (ID: {})", rawMaterial.getMaterialName(), rawMaterialId);
            throw new NoSuppliersFoundException(rawMaterialId, rawMaterial.getMaterialName());
        }

        // Step 3: Map to DTOs
        List<SupplierInfoDto> supplierInfos = rawMaterialSuppliers.stream()
                .map(this::mapToSupplierInfoDto)
                .collect(Collectors.toList());

        log.info("Found {} suppliers for raw material: {} (ID: {})",
                supplierInfos.size(), rawMaterial.getMaterialName(), rawMaterialId);

        // Step 4: Build response
        return RawMaterialSuppliersResponseDto.builder()
                .rawMaterialId(rawMaterialId)
                .rawMaterialName(rawMaterial.getMaterialName())
                .suppliers(supplierInfos)
                .totalSuppliersCount(supplierInfos.size())
                .build();
    }

    /**
     * Maps RawMaterialSupplier entity to SupplierInfoDto.
     */
    private SupplierInfoDto mapToSupplierInfoDto(RawMaterialSupplier rawMaterialSupplier) {
        return SupplierInfoDto.builder()
                .supplierId(rawMaterialSupplier.getSupplier().getSupplierId())
                .name(rawMaterialSupplier.getSupplier().getName())
                .address(rawMaterialSupplier.getSupplier().getAddress())
                .contactNumber(rawMaterialSupplier.getSupplier().getContactNumber())
                .email(rawMaterialSupplier.getSupplier().getEmail())
                .negotiatedUnitCost(rawMaterialSupplier.getNegotiatedUnitCost())
                .leadTimeDays(rawMaterialSupplier.getLeadTimeDays())
                .isPreferred(rawMaterialSupplier.getIsPreferred())
                .build();
    }
}
