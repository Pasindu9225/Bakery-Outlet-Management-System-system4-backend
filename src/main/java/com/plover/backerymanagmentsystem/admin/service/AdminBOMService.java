package com.plover.backerymanagmentsystem.admin.service;

import com.plover.backerymanagmentsystem.admin.dto.AdminBOMRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.AdminBOMResponseDto;
import java.util.List;

public interface AdminBOMService {
    List<AdminBOMResponseDto> getAllBOMs();

    AdminBOMResponseDto saveBOM(AdminBOMRequestDto requestDto);

    AdminBOMResponseDto updateBOM(Long parentProductId, AdminBOMRequestDto requestDto);

    void deleteBOM(Long parentProductId);

    void recalculateBOMMetrics(Long parentProductId);
}
