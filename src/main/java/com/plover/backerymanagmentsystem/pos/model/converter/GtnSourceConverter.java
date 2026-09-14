package com.plover.backerymanagmentsystem.pos.model.converter;

import com.plover.backerymanagmentsystem.pos.model.GtnSource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GtnSourceConverter implements AttributeConverter<GtnSource, String> {
	@Override
	public String convertToDatabaseColumn(GtnSource attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getValue();
	}

	@Override
	public GtnSource convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		for (GtnSource source : GtnSource.values()) {
			if (source.getValue().equalsIgnoreCase(dbData)) {
				return source;
			}
		}
		return GtnSource.UNDEFINED;
	}
}
