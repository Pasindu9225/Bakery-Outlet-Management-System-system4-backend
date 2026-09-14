package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponseDto {
    private List<String> labels;
    private List<TrendDatasetDto> datasets;
    private TrendSummaryDto summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDatasetDto {
        private String label;
        private List<Double> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendSummaryDto {
        private Double totalValue;
        private String percentageChange;
        private String peakDay;
        private Double peakValue;
        private Double averageDaily;
    }
}
