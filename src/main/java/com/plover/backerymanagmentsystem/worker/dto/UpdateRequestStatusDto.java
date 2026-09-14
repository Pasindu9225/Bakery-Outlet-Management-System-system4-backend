package com.plover.backerymanagmentsystem.worker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestStatusDto {
    @NotBlank(message = "Status is required")
    private String status; // IN_PROGRESS or COMPLETED
}
