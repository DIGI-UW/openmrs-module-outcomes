package org.openmrs.module.outcomes.web.controller;

import ClickSend.Api.SmsApi;
import ClickSend.ApiClient;
import ClickSend.ApiException;
import ClickSend.Model.SmsMessage;
import ClickSend.Model.SmsMessageCollection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.VisitService;
import org.openmrs.module.outcomes.OutcomesConstants;
import org.openmrs.module.outcomes.Questionaire;
import org.openmrs.module.outcomes.api.OutcomesService;
import org.openmrs.module.outcomes.api.resource.QuestionaireResource;
import org.openmrs.module.outcomes.request.MessageRequest;
import org.openmrs.module.outcomes.response.QuestionaireResponse;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.openmrs.obs.ComplexData;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.openmrs.module.outcomes.utils.OutcomesUtils.createComplexObs;
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
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private ApiClient clickSendConfig;
	
	ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	
	@RequestMapping(value = "/questionnaire/{questionnaireUuid}", method = RequestMethod.GET)
	public ResponseEntity<String> getQuestionnaire(@PathVariable String questionnaireUuid) {
		Questionaire questionaire = outcomesService.getQuestionnaireByUuid(Objects.requireNonNull(questionnaireUuid));
		return new ResponseEntity<>(questionaire != null ? questionaire.getResource() : null, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/questionnaire/{patientUuid}", method = RequestMethod.GET)
	public ResponseEntity<Double> getQuickDashDisabilityScore(@PathVariable String patientUuid) {
		Double dashScore = null;
		if (StringUtils.isNotEmpty(patientUuid)) {
			Patient patient = patientService.getPatientByUuid(patientUuid);
			EncounterType questionnaireEncounterType = encounterService.getEncounterTypeByUuid(
					OutcomesConstants.QUESTIONNAIRE_ENCOUNTER_TYPE_UUID);
			Optional<Encounter> encounter = encounterService.getEncountersByPatient(patient).stream()
					.filter(theEncounter -> theEncounter.getEncounterType()
							.equals(questionnaireEncounterType))
					.max(Comparator.comparing(Encounter::getEncounterDatetime));
			if (encounter.isPresent()) {
				Set<Obs> obsSet = encounter.get().getAllObs(false);
				int numberOfResponses = obsSet.size();
				if (numberOfResponses >= 3) {
					int score = ((numberOfResponses - 1) / numberOfResponses) * 25;
					dashScore = (double) score;
				}
			}
		}
		return new ResponseEntity<>(dashScore, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/sms", method = RequestMethod.POST, consumes="application/json")
	public ResponseEntity<String> sendSmsMessage(@RequestBody MessageRequest messageRequest) {
		Patient patient = patientService.getPatientByUuid(messageRequest.getPatientUuid());
		if (patient != null) {
			Person person = patient.getPerson();
			PersonAttributeType guidPersonAttributeType = personService
					.getPersonAttributeTypeByUuid(OutcomesConstants.GUID_PERSON_ATTRIBUTE_TYPE);
			PersonAttribute guidPersonAttribute = new PersonAttribute();
			guidPersonAttribute.setAttributeType(guidPersonAttributeType);
			guidPersonAttribute.setPerson(person);
			guidPersonAttribute.setValue(messageRequest.getGuid());
			guidPersonAttribute.setCreator(person.getCreator());
			person.addAttribute(guidPersonAttribute);
			personService.savePerson(person);
		}
		
		SmsApi smsApi = new SmsApi(clickSendConfig);
		SmsMessage smsMessage = new SmsMessage();
		smsMessage.body(messageRequest.getBody());
		smsMessage.to(messageRequest.getTo());
		smsMessage.source(messageRequest.getSource());

		List<SmsMessage> smsMessageList = new ArrayList<>();
		smsMessageList.add(smsMessage);
		SmsMessageCollection smsMessages = new SmsMessageCollection();
		smsMessages.messages(smsMessageList);
		
		try {
			return new ResponseEntity<>(smsApi.smsSendPost(smsMessages), HttpStatus.OK);
		} catch (ApiException exception) {
			log.info(exception.getMessage());
			return new ResponseEntity<>("Failed " + exception.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/questionnaire", method = RequestMethod.POST, consumes="application/json")
	public ResponseEntity<QuestionaireResponse> saveQuestionnaire(@Valid @RequestBody QuestionaireResource questionaireResource, final BindingResult bindingResult)
			throws JsonProcessingException {

		if (bindingResult.hasErrors()) {
			throw new APIException("An error occurred! Please contact System Administrator");
		}

		QuestionaireResponse questionaireResponse = new QuestionaireResponse();

		PersonAttributeType guidPersonAttributeType = personService
				.getPersonAttributeTypeByUuid(OutcomesConstants.GUID_PERSON_ATTRIBUTE_TYPE);

		Integer patientIdentifier = outcomesService.getPatientHavingPersonAttributes(guidPersonAttributeType,
				Collections.singletonList(questionaireResource.getGuid()));

		if (patientIdentifier != null) {
			Patient patient = patientService.getPatient(patientIdentifier);
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

					if (!Objects.isNull(questionaireResource.getPhoto())) {
						String json = mapper.writeValueAsString(questionaireResource);
						ComplexData complexImageData = null;
						try {
							JsonNode nodes = mapper.readTree(json).get("photo");
							for (JsonNode node : nodes) {
								complexImageData = new ComplexData(node.get("name").asText(), node.get("content"));
							}
						}
						catch (JsonProcessingException e) {
							throw new RuntimeException(e);
						}
						createComplexObs(questionnaireEncounter, getConcept(OutcomesConstants.INJURY_PHOTO_CONCEPT_UUID), complexImageData);
					}
				}
			}
			questionaireResponse.setMessage("Successfully saved Questionnaire for patient " + questionaireResource.getGuid());
			return new ResponseEntity<>(questionaireResponse, HttpStatus.CREATED);
		}
		questionaireResponse.setMessage("Failed to save Questionnaire!");
		return new ResponseEntity<>(questionaireResponse, HttpStatus.UNPROCESSABLE_ENTITY);
	}
}
