package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.BomResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BomTreeResponseDto;

public interface BomService {

    BomResponseDto getBomByParentProductId(Long parentProductId);

    BomTreeResponseDto getBomTree(Long parentProductId);
}


