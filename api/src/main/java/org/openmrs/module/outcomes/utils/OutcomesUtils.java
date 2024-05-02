package org.openmrs.module.outcomes.utils;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.util.OpenmrsUtil;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutcomesUtils {
	
	private static final LocationService locationService = Context.getLocationService();
	
	static Logger logger = Logger.getLogger(OutcomesUtils.class.getName());
	
	public static Visit getPatientActiveVisit(Patient patient, Location location, boolean ensureActive) {
		Visit currentVisit;
		AdtService adtService = Context.getService(AdtService.class);
		if (ensureActive) {
			currentVisit = adtService.ensureActiveVisit(patient, location);
		} else {
			VisitDomainWrapper wrapper = adtService.getActiveVisit(patient, location);
			currentVisit = wrapper == null ? null : wrapper.getVisit();
		}
		return currentVisit;
	}
	
	public static String formatDateWithoutTime(Date date, String format) {
		Format formatter = new SimpleDateFormat(format);
		return formatter.format(date);
	}
	
	public static String formatDateWithTime(Date date) {
		Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format(date);
	}
	
	public static Date formatDateFromString(String date, String format) throws ParseException {
		return new SimpleDateFormat(format).parse(date);
	}
	
	public static Date formatDateFromStringWithTime(String date) throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm a").parse(date);
	}
	
	public static Date formatFullDateFromStringWithTime(String date) throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(date);
	}
	
	public static ConceptAnswer getAnswerConcept(Set<ConceptAnswer> conceptAnswers,
	        Predicate<ConceptAnswer> educationLevelPredicate) {
		ConceptAnswer conceptAnswer = new ConceptAnswer();
		for (ConceptAnswer concept : conceptAnswers) {
			if (educationLevelPredicate.test(concept)) {
				conceptAnswer = concept;
			}
		}
		return conceptAnswer;
	}
	
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
				logger.log(Level.WARNING, lookup, e);
			}
		}
		return concept;
	}
	
	public static List<ConceptAnswer> getConceptAnswers(Concept concept) {
		List<ConceptAnswer> answerConcepts = null;
		if (concept != null) {
			answerConcepts = new ArrayList<>(concept.getAnswers());
			answerConcepts.sort((o1, o2) -> {
				Object obj1 = o1.getAnswerConcept()
						.getName()
						.getName();
				Object obj2 = o2.getAnswerConcept()
						.getName()
						.getName();
				if (o1.getAnswerConcept()
						.getName().getName().equals(o2.getAnswerConcept()
								.getName().getName()))
					return 0;
				return obj1.toString().compareToIgnoreCase(obj2.toString());
			});
		}
		return answerConcepts;
	}
	
	public static String formatPersonName(String givenName, String middleName, String familyName) {
		List<String> items = new ArrayList<>();
		if (StringUtils.isNotEmpty(familyName)) {
			items.add(familyName + " ");
		}
		if (StringUtils.isNotEmpty(givenName)) {
			items.add(givenName + " ");
		}
		if (StringUtils.isNotEmpty(middleName)) {
			items.add(middleName + " ");
		}
		return OpenmrsUtil.join(items, " ");
	}
	
	public static Location getLocation(String location) {
		Location currentlocation = null;
		if (StringUtils.isNotEmpty(location)) {
			currentlocation = locationService.getLocationByUuid(location);
			currentlocation = currentlocation == null ? locationService.getLocation(location) : currentlocation;
			currentlocation = currentlocation == null ? locationService.getLocation(Integer.valueOf(location))
			        : currentlocation;
		}
		return currentlocation;
	}
}
