package org.openmrs.module.outcomes.simplifier;

import lombok.Data;

@Data
public class SimplifiedPatient {
	
	private String name;
	
	private String gender;
	
	private String age;
	
	private String registeredBy;
	
	private String registeredDate;
	
	private String status;
	
	private String dateOfBirth;
	
	private String visitNumber;
	
	private Integer patientId;
	
	private String identifier;
}
