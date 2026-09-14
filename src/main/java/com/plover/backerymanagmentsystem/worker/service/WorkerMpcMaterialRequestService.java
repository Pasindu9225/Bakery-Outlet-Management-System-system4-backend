package com.plover.backerymanagmentsystem.worker.service;

import java.util.List;
import com.plover.backerymanagmentsystem.worker.dto.CreateMpcMaterialRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.MpcMaterialRequestResponseDto;

public interface WorkerMpcMaterialRequestService {
    MpcMaterialRequestResponseDto createRequest(CreateMpcMaterialRequestDto dto);
    List<MpcMaterialRequestResponseDto> listRequests();
    MpcMaterialRequestResponseDto acceptMaterials(Long id);
    MpcMaterialRequestResponseDto approveRequest(Long id);
    MpcMaterialRequestResponseDto rejectRequest(Long id);
}
