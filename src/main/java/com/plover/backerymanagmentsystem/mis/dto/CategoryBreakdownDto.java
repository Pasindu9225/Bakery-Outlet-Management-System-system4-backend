package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {label, value} bucket for the category-spend donut chart. {@code label}
 * is the {@code GenericMaterial.category} string (or {@code "Uncategorized"}
 * when no category is recorded for the underlying raw material).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryBreakdownDto {

    /** Category label as displayed on the slice. */
    private String label;

    /** Total spend (sum of line totals) for the category. */
    private BigDecimal value;
}
