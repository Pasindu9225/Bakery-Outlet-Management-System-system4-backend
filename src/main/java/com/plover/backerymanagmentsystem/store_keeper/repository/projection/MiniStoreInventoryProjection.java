package com.plover.backerymanagmentsystem.store_keeper.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MiniStoreInventoryProjection {

    Integer getItemId();

    String getName();

    Integer getMiniStoreId();

    String getMiniStoreName();

    Long getRawMaterialId();

    String getMaterialName();

    String getMaterialCode();

    String getUnitOfMeasure();

    Long getProductId();

    String getProductName();

    String getProductCode();

    Double getUnitPrice();

    BigDecimal getSystemQty();

    BigDecimal getPhysicalQty();

    BigDecimal getVariance();

    LocalDateTime getUpdatedAt();
}
