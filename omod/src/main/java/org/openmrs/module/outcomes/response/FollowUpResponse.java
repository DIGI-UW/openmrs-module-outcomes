package org.openmrs.module.outcomes.response;

import lombok.Data;

public @Data
class FollowUpResponse {
	
	String phoneNumber;
	
	String contactReason;
	
	String screeningDate;
	
	String modeOfContact;
}
