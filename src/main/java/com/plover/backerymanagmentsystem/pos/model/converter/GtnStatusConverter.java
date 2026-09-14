package com.plover.backerymanagmentsystem.pos.model.converter;

import com.plover.backerymanagmentsystem.pos.model.GtnStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GtnStatusConverter implements AttributeConverter<GtnStatus, String> {
	@Override
	public String convertToDatabaseColumn(GtnStatus attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getValue();
	}

	@Override
	public GtnStatus convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		for (GtnStatus status : GtnStatus.values()) {
			if (status.getValue().equalsIgnoreCase(dbData)) {
				return status;
			}
		}
		return GtnStatus.NOT_RECEIVED;
	}
}
