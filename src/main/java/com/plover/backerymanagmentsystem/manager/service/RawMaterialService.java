package com.plover.backerymanagmentsystem.manager.service;

import java.util.List;

import com.plover.backerymanagmentsystem.manager.dto.RawMaterialSummaryDto;

public interface RawMaterialService {

    List<RawMaterialSummaryDto> getAllRawMaterials();
}
