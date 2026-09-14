package com.plover.backerymanagmentsystem.pos.model.converter;

import com.plover.backerymanagmentsystem.pos.model.EntryStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EntryStatusConverter implements AttributeConverter<EntryStatus, String> {
	@Override
	public String convertToDatabaseColumn(EntryStatus attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getValue();
	}

	@Override
	public EntryStatus convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		for (EntryStatus status : EntryStatus.values()) {
			if (status.getValue().equalsIgnoreCase(dbData)) {
				return status;
			}
		}
		return EntryStatus.MANUAL;
	}
}
