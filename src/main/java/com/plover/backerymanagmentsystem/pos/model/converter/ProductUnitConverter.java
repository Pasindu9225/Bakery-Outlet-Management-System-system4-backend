package com.plover.backerymanagmentsystem.pos.model.converter;

import com.plover.backerymanagmentsystem.pos.model.ProductUnit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductUnitConverter implements AttributeConverter<ProductUnit, String> {
	@Override
	public String convertToDatabaseColumn(ProductUnit attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getValue();
	}

	@Override
	public ProductUnit convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		for (ProductUnit unit : ProductUnit.values()) {
			if (unit.getValue().equalsIgnoreCase(dbData)) {
				return unit;
			}
		}
		return ProductUnit.PIECES;
	}
}
