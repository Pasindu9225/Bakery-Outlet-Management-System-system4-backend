package com.plover.backerymanagmentsystem.admin.service.impl;

import com.plover.backerymanagmentsystem.admin.dto.CreateSupplierRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.SupplierDetailsDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateSupplierRequestDto;
import com.plover.backerymanagmentsystem.admin.service.AdminSupplierService;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupplierServiceImpl implements AdminSupplierService {

    private final SupplierRepository supplierRepository;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDetailsDto> getAllSupplierDetails() {
        log.info("Admin: fetching all supplier details with raw materials");
        
        List<Supplier> suppliers = supplierRepository.findAll();
        
        return suppliers.stream()
                .map(this::toSupplierDetailsDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupplierDetailsDto createSupplier(CreateSupplierRequestDto request) {
        log.info("Admin: creating new supplier with name: {}", request.getName());
        
        // Create new supplier
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .address(request.getAddress())
                .contactNumber(request.getContactNumber())
                .email(request.getEmail())
                .repName(request.getRepName())
                .repContactNo(request.getRepContactNo())
                .bankDetails(request.getBankDetails())
                .vatStatus(request.getVatStatus())
                .build();
        
        // Save supplier
        Supplier savedSupplier = supplierRepository.save(supplier);
        log.debug("Created supplier with id: {}", savedSupplier.getSupplierId());
        
        // Create raw material suppliers if provided
        if (request.getRawMaterials() != null && !request.getRawMaterials().isEmpty()) {
            List<RawMaterialSupplier> rawMaterialSuppliers = request.getRawMaterials().stream()
                    .map(rmDto -> {
                        // Validate raw material exists
                        Optional<RawMaterial> rawMaterialOpt = rawMaterialRepository.findById(rmDto.getRawMaterialId());
                        if (rawMaterialOpt.isEmpty()) {
                            throw new RuntimeException("Raw material not found with id: " + rmDto.getRawMaterialId());
                        }
                        
                        return RawMaterialSupplier.builder()
                                .rawMaterialId(rmDto.getRawMaterialId())
                                .supplierId(savedSupplier.getSupplierId())
                                .negotiatedUnitCost(rmDto.getNegotiatedUnitCost())
                                .leadTimeDays(rmDto.getLeadTimeDays())
                                .isPreferred(rmDto.getIsPreferred() != null ? rmDto.getIsPreferred() : false)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            rawMaterialSupplierRepository.saveAll(rawMaterialSuppliers);
            entityManager.flush(); // Flush to ensure entities are persisted
            log.debug("Created {} raw material supplier relationships", rawMaterialSuppliers.size());
        }
        
        // Return created supplier details
        return toSupplierDetailsDto(savedSupplier);
    }

    @Override
    @Transactional
    public SupplierDetailsDto updateSupplier(Long supplierId, UpdateSupplierRequestDto request) {
        log.info("Admin: updating supplier with id: {}", supplierId);
        
        // Find existing supplier
        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        if (supplierOpt.isEmpty()) {
            throw new RuntimeException("Supplier not found with id: " + supplierId);
        }
        
        Supplier supplier = supplierOpt.get();
        
        // Update supplier fields
        supplier.setName(request.getName());
        supplier.setAddress(request.getAddress());
        supplier.setContactNumber(request.getContactNumber());
        supplier.setEmail(request.getEmail());
        supplier.setRepName(request.getRepName());
        supplier.setRepContactNo(request.getRepContactNo());
        supplier.setBankDetails(request.getBankDetails());
        supplier.setVatStatus(request.getVatStatus());
        
        // Save updated supplier
        Supplier updatedSupplier = supplierRepository.save(supplier);
        
        // Delete existing raw material suppliers for this supplier
        List<RawMaterialSupplier> existingRawMaterialSuppliers = 
                rawMaterialSupplierRepository.findBySupplierId(supplierId);
        if (!existingRawMaterialSuppliers.isEmpty()) {
            rawMaterialSupplierRepository.deleteAll(existingRawMaterialSuppliers);
            log.debug("Deleted {} existing raw material supplier relationships", existingRawMaterialSuppliers.size());
        }
        
        // Create new raw material suppliers if provided
        if (request.getRawMaterials() != null && !request.getRawMaterials().isEmpty()) {
            List<RawMaterialSupplier> newRawMaterialSuppliers = request.getRawMaterials().stream()
                    .map(rmDto -> {
                        // Validate raw material exists
                        Optional<RawMaterial> rawMaterialOpt = rawMaterialRepository.findById(rmDto.getRawMaterialId());
                        if (rawMaterialOpt.isEmpty()) {
                            throw new RuntimeException("Raw material not found with id: " + rmDto.getRawMaterialId());
                        }
                        
                        return RawMaterialSupplier.builder()
                                .rawMaterialId(rmDto.getRawMaterialId())
                                .supplierId(supplierId)
                                .negotiatedUnitCost(rmDto.getNegotiatedUnitCost())
                                .leadTimeDays(rmDto.getLeadTimeDays())
                                .isPreferred(rmDto.getIsPreferred() != null ? rmDto.getIsPreferred() : false)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            rawMaterialSupplierRepository.saveAll(newRawMaterialSuppliers);
            entityManager.flush(); // Flush to ensure entities are persisted
            log.debug("Created {} new raw material supplier relationships", newRawMaterialSuppliers.size());
        }
        
        // Return updated supplier details
        return toSupplierDetailsDto(updatedSupplier);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long supplierId) {
        log.info("Admin: deleting supplier with id: {}", supplierId);

        // Check supplier exists
        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        if (supplierOpt.isEmpty()) {
            throw new RuntimeException("Supplier not found with id: " + supplierId);
        }

        // Delete raw material supplier relationships first
        List<RawMaterialSupplier> rawMaterialSuppliers =
                rawMaterialSupplierRepository.findBySupplierId(supplierId);
        if (!rawMaterialSuppliers.isEmpty()) {
            rawMaterialSupplierRepository.deleteAll(rawMaterialSuppliers);
            log.debug("Deleted {} raw material supplier relationships before deleting supplier", rawMaterialSuppliers.size());
        }

        try {
            // Delete supplier
            supplierRepository.deleteById(supplierId);
            supplierRepository.flush(); // Flush to trigger DataIntegrityViolationException immediately if constrained
            log.debug("Deleted supplier with id: {}", supplierId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Failed to delete supplier because it is referenced in other records.", e);
            throw new IllegalArgumentException("Cannot delete this supplier because it is used in active Purchase Orders or other records. Please remove these references first.");
        }
    }

    private SupplierDetailsDto toSupplierDetailsDto(Supplier supplier) {
        // Get all raw materials supplied by this supplier
        List<RawMaterialSupplier> rawMaterialSuppliers = 
                rawMaterialSupplierRepository.findBySupplierId(supplier.getSupplierId());
        
        // Map to DTO
        List<SupplierDetailsDto.RawMaterialInfoDto> rawMaterials = rawMaterialSuppliers.stream()
                .map(rms -> {
                    // Get raw material details from the relationship
                    var rawMaterial = rms.getRawMaterial();
                    if (rawMaterial == null) {
                        // If rawMaterial is null, fetch it manually
                        Optional<RawMaterial> rawMaterialOpt = rawMaterialRepository.findById(rms.getRawMaterialId());
                        if (rawMaterialOpt.isEmpty()) {
                            throw new RuntimeException("Raw material not found with id: " + rms.getRawMaterialId());
                        }
                        rawMaterial = rawMaterialOpt.get();
                    }
                    return SupplierDetailsDto.RawMaterialInfoDto.builder()
                            .rawMaterialId(rawMaterial.getId())
                            .materialName(rawMaterial.getMaterialName())
                            .materialCode(rawMaterial.getMaterialCode())
                            .category(rawMaterial.getCategory())
                            .brand(rawMaterial.getBrandName())
                            .negotiatedUnitCost(rms.getNegotiatedUnitCost())
                            .leadTimeDays(rms.getLeadTimeDays())
                            .isPreferred(rms.getIsPreferred())
                            .build();
                })
                .collect(Collectors.toList());
        
        return SupplierDetailsDto.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .address(supplier.getAddress())
                .contactNumber(supplier.getContactNumber())
                .email(supplier.getEmail())
                .repName(supplier.getRepName())
                .repContactNo(supplier.getRepContactNo())
                .bankDetails(supplier.getBankDetails())
                .vatStatus(supplier.getVatStatus())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .rawMaterials(rawMaterials)
                .build();
    }
}

