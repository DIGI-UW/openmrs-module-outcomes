package org.openmrs.module.outcomes.simplifier;

import lombok.Data;
import org.openmrs.Patient;
import org.openmrs.Visit;

@Data
public class SimplifiedVisit {
	
	private Integer visitId;
	
	private Patient patient;
	
	private Visit visit;
}
