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
import java.util.stream.Collectors;

import static org.openmrs.module.outcomes.OutcomesConstants.EXTREMELY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.EXTREME_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.MILD_DIFFICULTY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.MILD_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.MODERATELY_LIMITED_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.MODERATELY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.MODERATE_DIFFICULTY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.MODERATE_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.NONE_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.NOT_AT_ALL_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.NOT_LIMITED_AT_ALL_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.NO_DIFFICULTY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.QUITE_A_BIT_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.SEVERE_DIFFICULTY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.SEVERE_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.SLIGHTLY_LIMITED_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.SLIGHTLY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.SO_MUCH_DIFFICULTY_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.UNABLE_RESPONSE_UUID;
import static org.openmrs.module.outcomes.OutcomesConstants.VERY_LIMITED_RESPONSE_UUID;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.createEncounter;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.createObs;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.createVisit;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.getConcept;
import static org.openmrs.module.outcomes.utils.OutcomesUtils.createComplexObs;

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
	
	@RequestMapping(value = "/score/{patientUuid}", method = RequestMethod.GET)
	public ResponseEntity<Double> getQuickDashDisabilityScore(@PathVariable String patientUuid) {
		double dashScore = 0.0;
		if (StringUtils.isNotEmpty(patientUuid)) {
			Patient patient = patientService.getPatientByUuid(patientUuid);
			EncounterType questionnaireEncounterType = encounterService.getEncounterTypeByUuid(
					OutcomesConstants.QUESTIONNAIRE_ENCOUNTER_TYPE_UUID);
			Optional<Encounter> encounter = encounterService.getEncountersByPatient(patient).stream()
					.filter(theEncounter -> theEncounter.getEncounterType()
							.equals(questionnaireEncounterType))
					.max(Comparator.comparing(Encounter::getEncounterDatetime));
			if (encounter.isPresent()) {
				Set<Obs> obsSet = encounter.get()
						.getAllObs(false)
						.stream()
						.filter(obs -> obs.getValueCoded() != null)
						.collect(Collectors.toSet());
				int numberOfResponses = obsSet.size();
				double sumOfResponses = 0.0;
				for(Obs obs : obsSet) {
					String conceptUuid = obs.getValueCoded().getUuid();
					if (conceptUuid.equals(NO_DIFFICULTY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, NO_DIFFICULTY_RESPONSE_UUID, 1, sumOfResponses);
					}					
					if (conceptUuid.equals(MILD_DIFFICULTY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, MILD_DIFFICULTY_RESPONSE_UUID, 2, sumOfResponses);
					}
					if (conceptUuid.equals(MODERATE_DIFFICULTY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, MODERATE_DIFFICULTY_RESPONSE_UUID, 3, sumOfResponses);
					}
					if (conceptUuid.equals(SEVERE_DIFFICULTY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, SEVERE_DIFFICULTY_RESPONSE_UUID, 4, sumOfResponses);
					}
					if (conceptUuid.equals(UNABLE_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, UNABLE_RESPONSE_UUID, 5, sumOfResponses);
					}
					if (conceptUuid.equals(NOT_AT_ALL_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, NOT_AT_ALL_RESPONSE_UUID, 1, sumOfResponses);
					}
					if (conceptUuid.equals(SLIGHTLY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, SLIGHTLY_RESPONSE_UUID, 2, sumOfResponses);
					}					
					if (conceptUuid.equals(MODERATELY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, MODERATELY_RESPONSE_UUID, 3, sumOfResponses);
					}
					if (conceptUuid.equals(QUITE_A_BIT_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, QUITE_A_BIT_RESPONSE_UUID, 4, sumOfResponses);
					}
					if (conceptUuid.equals(EXTREMELY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, EXTREMELY_RESPONSE_UUID, 5, sumOfResponses);
					}
					if (conceptUuid.equals(NOT_LIMITED_AT_ALL_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, NOT_LIMITED_AT_ALL_RESPONSE_UUID, 1, sumOfResponses);
					}
					if (conceptUuid.equals(SLIGHTLY_LIMITED_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, SLIGHTLY_LIMITED_RESPONSE_UUID, 2, sumOfResponses);
					}
					if (conceptUuid.equals(MODERATELY_LIMITED_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, MODERATELY_LIMITED_RESPONSE_UUID, 3, sumOfResponses);
					}
					if (conceptUuid.equals(VERY_LIMITED_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, VERY_LIMITED_RESPONSE_UUID, 4, sumOfResponses);
					}
					if (conceptUuid.equals(UNABLE_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, UNABLE_RESPONSE_UUID, 5, sumOfResponses);
					}
					if (conceptUuid.equals(NONE_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, NONE_RESPONSE_UUID, 1, sumOfResponses);
					}
					if (conceptUuid.equals(MILD_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, MILD_RESPONSE_UUID, 2, sumOfResponses);
					}
					if (conceptUuid.equals(MODERATE_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, MODERATE_RESPONSE_UUID, 3, sumOfResponses);
					}
					if (conceptUuid.equals(SEVERE_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, SEVERE_RESPONSE_UUID, 4, sumOfResponses);
					}
					if (conceptUuid.equals(EXTREME_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, EXTREME_RESPONSE_UUID, 5, sumOfResponses);
					}
					if (conceptUuid.equals(SO_MUCH_DIFFICULTY_RESPONSE_UUID)) {
						sumOfResponses = getSumOfResponses(conceptUuid, SO_MUCH_DIFFICULTY_RESPONSE_UUID, 5, sumOfResponses);
					}
				}
				dashScore = ((sumOfResponses - 1) / numberOfResponses) * 25;
			}
		}
		return new ResponseEntity<>(dashScore, HttpStatus.OK);
	}
	
	private static double getSumOfResponses(String conceptUuid, String responseUuid, int counter, double sumOfResponses) {
		if (conceptUuid.equals(responseUuid)) {
			sumOfResponses += counter;
		}
		return sumOfResponses;
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
						String imageName = null;
						String base64Image = null;
						try {
							JsonNode nodes = mapper.readTree(json).get("photo");
							for (JsonNode node : nodes) {
								base64Image = node.get("content").asText();
								imageName = node.get("name").asText();
							}
						}
						catch (JsonProcessingException e) {
							throw new RuntimeException(e);
						}
						createComplexObs(questionnaireEncounter, getConcept(OutcomesConstants.INJURY_PHOTO_CONCEPT_UUID), base64Image, imageName);
					}
				}
			}
			questionaireResponse.setMessage("Successfully saved Questionnaire for patient " + questionaireResource.getGuid());
			return new ResponseEntity<>(questionaireResponse, HttpStatus.CREATED);
		}
		questionaireResponse.setMessage("Failed to save Questionnaire!");
		return new ResponseEntity<>(questionaireResponse, HttpStatus.UNPROCESSABLE_ENTITY);
	}
	
	@RequestMapping(value = "/footQuestionnaire", method = RequestMethod.POST, consumes="application/json")
	public ResponseEntity<QuestionaireResponse> saveFootQuestionnaire(@Valid @RequestBody QuestionaireResource questionaireResource, final BindingResult bindingResult)
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
					encounterService.getEncounterTypeByUuid(OutcomesConstants.FOOT_QUESTIONNAIRE_ENCOUNTER_TYPE_UUID);
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

						if (StringUtils.isNotEmpty(questionaireResource.getVasPainScale())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.VAS_PAIN_SCALE_CONCEPT_UUID), getConcept(questionaireResource.getVasPainScale()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getGeneralHealth())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.GENERAL_HEALTH_CONCEPT_UUID), getConcept(questionaireResource.getGeneralHealth()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getModerateEfforts())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.MODERATE_EFFORTS_CONCEPT_UUID), getConcept(questionaireResource.getModerateEfforts()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getClimbFloors())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.CLIMB_FLOORS_CONCEPT_UUID), getConcept(questionaireResource.getClimbFloors()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getAccomplishedLess())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.ACCOMPLISHED_LESS_CONCEPT_UUID), getConcept(questionaireResource.getAccomplishedLess()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getStoppedDoingWorkDaily())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.STOPPED_DOING_WORK_DAILY_CONCEPT_UUID), getConcept(questionaireResource.getStoppedDoingWorkDaily()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getLessAccomplishedThanYouWouldLikeEmotion())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.LESS_ACCOMPLISHED_THAN_YOU_WOULD_LIKE_EMOTION_CONCEPT_UUID), getConcept(questionaireResource.getLessAccomplishedThanYouWouldLikeEmotion()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getUnableToDoWorkOrOtherActivitiesCarefullyAsUsual())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.UNABLE_TO_DO_WORK_OR_OTHER_ACTIVITIES_CAREFULLY_AS_USUAL_CONCEPT_UUID), getConcept(questionaireResource.getUnableToDoWorkOrOtherActivitiesCarefullyAsUsual()));
						}
					
						if (StringUtils.isNotEmpty(questionaireResource.getAmountOfPainUnableToDoWork())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.AMOUNT_OF_PAIN_UNABLE_TO_DO_WORK_CONCEPT_UUID), getConcept(questionaireResource.getAmountOfPainUnableToDoWork()));
						}
						
						if (StringUtils.isNotEmpty(questionaireResource.getHaveYouFeltCalmAndPeaceful())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.HAVE_YOU_FELT_CALM_AND_PEACEFUL_CONCEPT_UUID), getConcept(questionaireResource.getHaveYouFeltCalmAndPeaceful()));
						}
						
						if (StringUtils.isNotEmpty(questionaireResource.getHadALotOfEnergy())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.HAD_A_LOT_OF_ENERGY_CONCEPT_UUID), getConcept(questionaireResource.getHadALotOfEnergy()));
						}
						
						if (StringUtils.isNotEmpty(questionaireResource.getFeltDiscouragedAndSad())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.FELT_DISCOURAGED_AND_SAD_CONCEPT_UUID), getConcept(questionaireResource.getFeltDiscouragedAndSad()));
						}
						
						if (StringUtils.isNotEmpty(questionaireResource.getInterferenceWithSocialActivities())) {
							createObs(questionnaireEncounter, getConcept(OutcomesConstants.INTERFERENCE_WITH_SOCIAL_ACTIVITIES_CONCEPT_UUID), getConcept(questionaireResource.getInterferenceWithSocialActivities()));
						}

					if (!Objects.isNull(questionaireResource.getPhoto())) {
						String json = mapper.writeValueAsString(questionaireResource);
						String imageName = null;
						String base64Image = null;
						try {
							JsonNode nodes = mapper.readTree(json).get("photo");
							for (JsonNode node : nodes) {
								base64Image = node.get("content").asText();
								imageName = node.get("name").asText();
							}
						}
						catch (JsonProcessingException e) {
							throw new RuntimeException(e);
						}
						createComplexObs(questionnaireEncounter, getConcept(OutcomesConstants.INJURY_PHOTO_CONCEPT_UUID), base64Image, imageName);
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
