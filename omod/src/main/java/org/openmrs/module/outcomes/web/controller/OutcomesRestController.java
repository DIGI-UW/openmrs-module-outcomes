package org.openmrs.module.outcomes.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.module.outcomes.OutcomesConstants;
import org.openmrs.module.outcomes.simplifier.SimplifiedPatient;
import org.openmrs.module.outcomes.utils.OutcomesUtils;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/" + OutcomesConstants.OUTCOMES_MODULE_ID)
public class OutcomesRestController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	private PatientService patientService;
	
	@GetMapping(value = "/case/{patientId}")
	public ResponseEntity<SimplifiedPatient> getCase(@PathVariable Integer patientId) {
		log.info("Querying the case endpoint!");
		SimplifiedPatient simplifiedPatient = new SimplifiedPatient();
		if (patientId != null) {
			Patient patient = patientService.getPatient(patientId);
			if (patient != null) {
				PersonName personName = patient.getPersonName();
				simplifiedPatient.setName(OutcomesUtils.formatPersonName(
								personName.getGivenName(),
								personName.getMiddleName(),
								personName.getFamilyName()));
				simplifiedPatient.setGender(patient.getGender());
				simplifiedPatient.setAge(String.valueOf(patient.getAge()));
				simplifiedPatient.setRegisteredBy(patient.getCreator()
						.getDisplayString());
				simplifiedPatient.setRegisteredDate(
						OutcomesUtils.formatDateWithoutTime(patient.getDateCreated(), "yyyy-MM-dd"));
				simplifiedPatient.setStatus((patient.getVoided()
						.equals(false) ? "Active" : "Inactive"));
				simplifiedPatient.setPatientId(patient.getPatientId());
			}
		}
		return new ResponseEntity<>(simplifiedPatient, HttpStatus.OK);
	}
}
