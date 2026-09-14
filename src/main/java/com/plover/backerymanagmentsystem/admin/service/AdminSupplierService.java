package com.plover.backerymanagmentsystem.admin.service;

import com.plover.backerymanagmentsystem.admin.dto.CreateSupplierRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.SupplierDetailsDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateSupplierRequestDto;

import java.util.List;

public interface AdminSupplierService {
    List<SupplierDetailsDto> getAllSupplierDetails();
    SupplierDetailsDto createSupplier(CreateSupplierRequestDto request);
    SupplierDetailsDto updateSupplier(Long supplierId, UpdateSupplierRequestDto request);
    void deleteSupplier(Long supplierId);
}

