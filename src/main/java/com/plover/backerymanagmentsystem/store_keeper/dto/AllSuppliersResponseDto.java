package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for retrieving all suppliers from the system. Contains a list of
 * suppliers with their complete information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllSuppliersResponseDto {

    private List<SupplierDto> suppliers;
    private Integer totalCount;

    /**
     * DTO representing individual supplier information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDto {

        private Long supplierId;
        private String name;
        private String address;
        private String contactNumber;
        private String email;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
