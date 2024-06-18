package org.openmrs.module.outcomes.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.outcomes.OutcomesConstants;
import org.openmrs.obs.ComplexData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OutcomesUtils {
	
	private static final LocationService locationService = Context.getLocationService();
	
	protected static final Log log = LogFactory.getLog(OutcomesUtils.class);
	
	public static Concept getConcept(String lookup) {
		Concept concept = Context.getConceptService().getConceptByUuid(lookup);
		if (concept == null) {
			concept = Context.getConceptService().getConceptByName(lookup);
		}
		if (concept == null) {
			try {
				String[] split = lookup.split("\\:");
				if (split.length == 2) {
					concept = Context.getConceptService().getConceptByMapping(split[1], split[0]);
				}
			}
			catch (Exception ignored) {}
		}
		if (concept == null) {
			try {
				concept = Context.getConceptService().getConcept(Integer.parseInt(lookup));
			}
			catch (Exception e) {
				log.info(e);
			}
		}
		return concept;
	}
	
	public static Encounter createEncounter(Patient patient, EncounterType encounterType, Visit visit) {
		Encounter encounter = null;
		try {
			User user = Context.getAuthenticatedUser();
			if (user != null) {
				Location location = locationService.getLocationByUuid(OutcomesConstants.UNKNOWN_LOCATION_UUID);
				if (visit != null) {
					encounter = new Encounter();
					encounter.setEncounterType(encounterType);
					encounter.setCreator(user);
					encounter.setProvider(getDefaultEncounterRole(), getProvider(user.getPerson()));
					encounter.setPatient(patient);
					encounter.setLocation(location);
					encounter.setEncounterDatetime(new Date());
					encounter.setVisit(visit);
					Context.getEncounterService().saveEncounter(encounter);
				}
			}
		}
		catch (Exception ex) {
			log.info("Failed to create encounter!");
		}
		return encounter;
	}
	
	public static EncounterRole getDefaultEncounterRole() {
		return Context.getEncounterService().getEncounterRoleByUuid(OutcomesConstants.UNKNOWN_ENCOUNTER_ROLE_UUID);
	}
	
	public static Provider getProvider(Person person) {
		Provider provider = null;
		List<Provider> providerList = new ArrayList<>(Context.getProviderService()
				.getProvidersByPerson(person));
		if (!providerList.isEmpty()) {
			provider = providerList.get(0);
		}
		return provider;
	}
	
	public static Obs createObs(Encounter encounter, Concept question, Concept conceptAnswer) {
		Obs obs = new Obs();
		obs.setPerson(encounter.getPatient());
		obs.setLocation(encounter.getLocation());
		obs.setCreator(encounter.getCreator());
		obs.setDateCreated(encounter.getDateCreated());
		obs.setEncounter(encounter);
		obs.setObsDatetime(encounter.getEncounterDatetime());
		if (question != null && conceptAnswer != null) {
			obs.setConcept(question);
			obs.setValueCoded(conceptAnswer);
			Context.getObsService().saveObs(obs, "Saved obs!");
		}
		return obs;
	}
	
	public static Obs createComplexObs(Encounter encounter, Concept question, ComplexData complexData) {
		Obs obs = new Obs();
		obs.setPerson(encounter.getPatient());
		obs.setLocation(encounter.getLocation());
		obs.setCreator(encounter.getCreator());
		obs.setDateCreated(encounter.getDateCreated());
		obs.setEncounter(encounter);
		obs.setObsDatetime(encounter.getEncounterDatetime());
		if (question != null && complexData != null) {
			obs.setConcept(question);
			obs.setComplexData(complexData);
			Context.getObsService().saveObs(obs, "Saved obs!");
		}
		return obs;
	}
	
	public static Visit createVisit(Patient patient, VisitType visitType, Date startTime) {
		Visit visit = new Visit(patient, visitType, startTime);
		Location location = locationService.getLocationByUuid(OutcomesConstants.UNKNOWN_LOCATION_UUID);
		visit.setLocation(location);
		return Context.getVisitService().saveVisit(visit);
	}
}
