package org.openmrs.module.outcomes.utils;

import java.util.UUID;

public class UuidGenerator {
	
	public static String getNextUuid() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
}
