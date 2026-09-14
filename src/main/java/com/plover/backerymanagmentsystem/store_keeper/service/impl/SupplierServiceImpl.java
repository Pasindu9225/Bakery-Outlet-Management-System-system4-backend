package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.store_keeper.dto.AllSuppliersResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllSuppliersResponseDto.SupplierDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.SuppliersNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of SupplierService for managing supplier operations. Handles
 * retrieval of supplier information with proper business logic and error
 * handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public AllSuppliersResponseDto getAllSuppliers() {
        log.info("Retrieving all suppliers from the system");

        List<Supplier> suppliers = supplierRepository.findAll();

        if (suppliers.isEmpty()) {
            log.warn("No suppliers found in the system");
            throw new SuppliersNotFoundException("No suppliers found in the system");
        }

        List<SupplierDto> supplierDtos = suppliers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} suppliers", suppliers.size());

        return AllSuppliersResponseDto.builder()
                .suppliers(supplierDtos)
                .totalCount(suppliers.size())
                .build();
    }

    /**
     * Converts a Supplier entity to SupplierDto.
     *
     * @param supplier the supplier entity to convert
     * @return the converted SupplierDto
     */
    private SupplierDto convertToDto(Supplier supplier) {
        return SupplierDto.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .address(supplier.getAddress())
                .contactNumber(supplier.getContactNumber())
                .email(supplier.getEmail())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
