package com.plover.backerymanagmentsystem.admin.service;

import java.util.List;

import com.plover.backerymanagmentsystem.manager.model.Outlet;

import com.plover.backerymanagmentsystem.admin.dto.CreateOutletRequest;
import com.plover.backerymanagmentsystem.admin.dto.UpdateOutletRequest;

public interface AdminOutletService {
    List<Outlet> getAllOutlets();

    Outlet createOutlet(CreateOutletRequest request);

    Outlet updateOutlet(Long id, UpdateOutletRequest request);

    void deleteOutlet(Long id);
}
