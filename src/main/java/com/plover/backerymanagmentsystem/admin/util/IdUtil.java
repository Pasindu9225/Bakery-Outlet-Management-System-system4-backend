package com.plover.backerymanagmentsystem.admin.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class IdUtil {
	private IdUtil() {}

	public static String bytesToUuidString(byte[] bytes) {
		UUID uuid = bytesToUuid(bytes);
		return uuid != null ? uuid.toString() : null;
	}

	public static UUID bytesToUuid(byte[] bytes) {
		if (bytes == null || bytes.length != 16) return null;
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		long high = bb.getLong();
		long low = bb.getLong();
		return new UUID(high, low);
	}

	public static byte[] uuidToBytes(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
}


