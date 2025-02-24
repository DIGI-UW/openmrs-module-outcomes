package org.openmrs.module.outcomes.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
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
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.attachments.ComplexObsSaver;
import org.openmrs.module.outcomes.OutcomesConstants;
import org.springframework.web.multipart.MultipartFile;

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
	
	public static Obs createComplexObs(Encounter encounter, Concept question, String base64Data, String imageName) {
		Obs obs = new Obs();
		obs.setPerson(encounter.getPatient());
		obs.setLocation(encounter.getLocation());
		obs.setCreator(encounter.getCreator());
		obs.setDateCreated(encounter.getDateCreated());
		obs.setEncounter(encounter);
		obs.setObsDatetime(encounter.getEncounterDatetime());
		if (question != null && base64Data != null) {
			obs.setConcept(question);
			
			MultipartFile file = null;
			try {
				file = new Base64MultipartFile(base64Data, imageName, imageName);
			}
			catch (IOException ex) {}
			
			try {
				
				Context.getRegisteredComponents(ComplexObsSaver.class)
				        .get(0)
				        .saveImageAttachment(encounter.getVisit(), encounter.getPatient(), encounter, imageName, file,
				            imageName);
			}
			catch (APIException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		return obs;
	}
	
	public static Visit createVisit(Patient patient, VisitType visitType, Date startTime) {
		Visit visit = new Visit(patient, visitType, startTime);
		Location location = locationService.getLocationByUuid(OutcomesConstants.UNKNOWN_LOCATION_UUID);
		visit.setLocation(location);
		return Context.getVisitService().saveVisit(visit);
	}
	
	static final class Base64MultipartFile implements MultipartFile {
		
		private final String fileName;
		
		private final String originalFileName;
		
		private final String contentType;
		
		private final long size;
		
		private final InputStream in;
		
		private final byte[] bytes;
		
		public Base64MultipartFile(String base64Image, String fileName, String originalFileName) throws IOException {
			String[] parts = base64Image.split(",", 2);
			String contentType = parts[0].split(":")[1].split(";")[0].trim();
			String contents = parts[1].trim();
			byte[] decodedImage = Base64.decodeBase64(contents.getBytes());
			
			this.fileName = fileName;
			this.originalFileName = originalFileName;
			this.in = new ByteArrayInputStream(decodedImage);
			this.contentType = contentType;
			this.bytes = decodedImage;
			this.size = decodedImage.length;
		}
		
		@Override
		public String getName() {
			return this.fileName;
		}
		
		@Override
		public String getOriginalFilename() {
			return this.originalFileName;
		}
		
		@Override
		public String getContentType() {
			return this.contentType;
		}
		
		@Override
		public boolean isEmpty() {
			return false;
		}
		
		@Override
		public long getSize() {
			return this.size;
		}
		
		@Override
		public byte[] getBytes() {
			return this.bytes;
		}
		
		@Override
		public InputStream getInputStream() {
			return this.in;
		}
		
		@Override
		public void transferTo(File dest) throws IllegalStateException {
			throw new APIException("Operation transferTo is not supported for Base64MultipartFile");
		}
	}
	
}
