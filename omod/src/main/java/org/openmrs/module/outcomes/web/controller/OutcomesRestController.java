package org.openmrs.module.outcomes.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.outcomes.OutcomesConstants;
import org.openmrs.module.outcomes.Questionaire;
import org.openmrs.module.outcomes.api.OutcomesService;
import org.openmrs.module.outcomes.api.resource.QuestionaireResource;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.Date;
import java.util.Objects;

import static org.openmrs.module.outcomes.utils.OutcomesUtils.createEncounter;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.createObs;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.createVisit;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.getConcept;

@Controller(value = "outcomesRestController")
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/" + OutcomesConstants.OUTCOMES_MODULE_ID)
public class OutcomesRestController extends MainResourceController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private EncounterService encounterService;
	
	@Autowired
	private OutcomesService outcomesService;
	
	ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	
	@RequestMapping(value = "/questionnaire/{questionnaireUuid}", method = RequestMethod.GET)
	public ResponseEntity<String> getQuestionnaire(@PathVariable String questionnaireUuid) {
		Questionaire questionaire = outcomesService.getQuestionnaireByUuid(Objects.requireNonNull(questionnaireUuid));
		return new ResponseEntity<>(questionaire != null ? questionaire.getResource() : "", HttpStatus.OK);
	}
	
	@RequestMapping(value = "/questionnaire", method = RequestMethod.POST, consumes="application/json")
	public ResponseEntity<QuestionaireResource> saveQuestionnaire(@Valid @RequestBody QuestionaireResource questionaireResource, final BindingResult bindingResult)
			throws JsonProcessingException {

		if (bindingResult.hasErrors()) {
			throw new APIException("An error occurred! Please contact System Administrator");
		}

		Patient patient = patientService.getPatient(questionaireResource.getPatientId());

		EncounterType questionnaireEncounterType =
				encounterService.getEncounterTypeByUuid(OutcomesConstants.QUESTIONNAIRE_ENCOUNTER_TYPE_UUID);

		VisitType visitType = visitService
				.getVisitTypeByUuid(OutcomesConstants.VISIT_TYPE_UUID);
		
		if (patient != null) {
			Questionaire questionaire = new Questionaire();
			questionaire.setResource(mapper.writeValueAsString(questionaireResource));
			outcomesService.saveQuestionnaire(questionaire);
			log.info("Saved questionnaire ".concat(mapper.writeValueAsString(questionaireResource)));
			
			Visit visit = createVisit(patient, visitType, new Date());
			if (questionnaireEncounterType != null) {
				Encounter questionnaireEncounter = createEncounter(patient, questionnaireEncounterType, visit);
				if (StringUtils.isNotEmpty(questionaireResource.getOpenTightJar())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.OPEN_TIGHT_JAR_CONCEPT_UUID), getConcept(questionaireResource.getOpenTightJar()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getHeavyHouseholdChores())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.HEAVY_HOUSEHOLD_CHORES_CONCEPT_UUID), getConcept(questionaireResource.getHeavyHouseholdChores()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getCarryShoppingBag())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.CARRY_SHOPPING_BAG_CONCEPT_UUID), getConcept(questionaireResource.getCarryShoppingBag()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getWashYourBack())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.WASH_YOUR_BACK_CONCEPT_UUID), getConcept(questionaireResource.getWashYourBack()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getUseKnifeToCutFood())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.USE_KNIFE_CONCEPT_UUID), getConcept(questionaireResource.getUseKnifeToCutFood()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getImpactfulRecreationalActivities())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.RECREATIONAL_ACTIVITIES_CONCEPT_UUID), getConcept(questionaireResource.getImpactfulRecreationalActivities()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getInterferenceWithSocialActivities())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.SOCIAL_ACTIVITIES_CONCEPT_UUID), getConcept(questionaireResource.getInterferenceWithSocialActivities()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getLimitationsInWorkActivities())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.ACTIVITY_LIMITATIONS_CONCEPT_UUID), getConcept(questionaireResource.getLimitationsInWorkActivities()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getArmShoulderHandPain())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.LIMB_PAIN_CONCEPT_UUID), getConcept(questionaireResource.getArmShoulderHandPain()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getTinglingPinsAndNeedles())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.TINGLING_NEEDLES_CONCEPT_UUID), getConcept(questionaireResource.getTinglingPinsAndNeedles()));
				}

				if (StringUtils.isNotEmpty(questionaireResource.getDifficultySleeping())) {
					createObs(questionnaireEncounter, getConcept(OutcomesConstants.DIFFICULTY_SLEEPING_CONCEPT_UUID), getConcept(questionaireResource.getDifficultySleeping()));
				}
			}
		}
		return new ResponseEntity<>(questionaireResource, HttpStatus.CREATED);
	}
}
