package com.plover.backerymanagmentsystem.manager.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletAllocationRequestDto {

	private Long outletId; // Optional: use if existing

	// Example format: "Outlet Name - Address"
	private String outletName; // Used to create/update if outletId is null

	private Boolean isActive;

	@Min(value = 1)
	@com.fasterxml.jackson.annotation.JsonAlias({"outletQty", "quantity"})
	private Integer quantity;

	private LocalDate date; // Distribution date for this allocation
}


