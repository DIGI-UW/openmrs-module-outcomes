package org.openmrs.module.outcomes.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.outcomes.Questionaire;
import org.openmrs.module.outcomes.api.OutcomesService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestConstants.VERSION_1 + "/questionaire", supportedClass = Questionaire.class, supportedOpenmrsVersions = {
        "2.0.*", "2.1.*", "2.2.*", "2.3.*", "2.4.*", "2.5.*" })
public class OutcomesResource extends DataDelegatingCrudResource<Questionaire> {
	
	private final OutcomesService outcomesService = Context.getService(OutcomesService.class);
	
	@Override
	public Questionaire getByUniqueId(String uniqueId) {
		return outcomesService.getQuestionnaireByUuid(uniqueId);
	}
	
	@Override
	protected void delete(Questionaire questionaire, String reason, RequestContext requestContext) throws ResponseException {
		if (questionaire.getVoided()) {
			return;
		}
		outcomesService.voidQuestionnaire(questionaire, reason);
	}
	
	@Override
	public Questionaire newDelegate() {
		return new Questionaire();
	}
	
	@Override
	public Questionaire save(Questionaire questionaire) {
		return outcomesService.saveQuestionnaire(questionaire);
	}
	
	@Override
	public void purge(Questionaire questionaire, RequestContext requestContext) throws ResponseException {
		outcomesService.purgeQuestionnaire(questionaire);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		if (representation instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("resource", Representation.FULL);
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		} else if (representation instanceof FullRepresentation) {
			return getDelegatingResourceDescription();
		} else {
			return null;
		}
	}
	
	private static DelegatingResourceDescription getDelegatingResourceDescription() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("resource", Representation.FULL);
		description.addSelfLink();
		return description;
	}
}
