package com.plover.backerymanagmentsystem.admin.service;

import java.util.List;

import com.plover.backerymanagmentsystem.admin.dto.CreateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.OutletProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.UpdateOutletProductionCenterRequest;

public interface AdminOutletProductionCenterService {

    OutletProductionCenterResponse create(Long outletId, CreateOutletProductionCenterRequest request);

    List<OutletProductionCenterResponse> listForOutlet(Long outletId);

    OutletProductionCenterResponse update(Long id, UpdateOutletProductionCenterRequest request);

    void delete(Long id);
}
