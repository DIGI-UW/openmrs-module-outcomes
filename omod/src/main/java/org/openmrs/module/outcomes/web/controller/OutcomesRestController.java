package org.openmrs.module.outcomes.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.outcomes.OutcomesConstants;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/" + OutcomesConstants.OUTCOMES_MODULE_ID)
public class OutcomesRestController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	private PatientService patientService;

	@GetMapping(value = "/endpoint")
	public ResponseEntity<List<Patient>> getPatientReminders() {
		log.info("Querying the dummy endpoint!");
		return new ResponseEntity<>(patientService.getAllPatients(), HttpStatus.OK);
	}
}
