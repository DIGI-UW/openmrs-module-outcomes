package org.openmrs.module.outcomes.request;

import lombok.Data;

public @Data
class MessageRequest {
	
	String to;
	
	String guid;
	
	String body;
	
	String source;
	
	String patientUuid;
}
