/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.outcomes.api.impl;

import lombok.Setter;
import org.openmrs.Patient;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.outcomes.Questionaire;
import org.openmrs.module.outcomes.api.OutcomesService;
import org.openmrs.module.outcomes.api.dao.OutcomesDao;
import org.springframework.util.StringUtils;

import java.util.List;

@Setter
public class OutcomesServiceImpl extends BaseOpenmrsService implements OutcomesService {
	
	/**
	 * -- SETTER -- Injected in moduleApplicationContext.xml
	 */
	OutcomesDao dao;
	
	@Override
	public Questionaire getQuestionnaireByUuid(String uuid) throws APIException {
		return dao.getQuestionnaireByUuid(uuid);
	}
	
	@Override
	public Questionaire saveQuestionnaire(Questionaire questionaire) throws APIException {
		return dao.saveQuestionnaire(questionaire);
	}
	
	@Override
	public Questionaire voidQuestionnaire(Questionaire questionaire, String reason) throws APIException {
		if (!StringUtils.hasLength(reason)) {
			throw new IllegalArgumentException("Reason cannot be empty or null");
		} else {
			questionaire.setVoided(true);
			return this.saveQuestionnaire(questionaire);
		}
	}
	
	@Override
	public void purgeQuestionnaire(Questionaire questionaire) throws APIException {
		dao.deleteQuestionnaire(questionaire);
	}
	
	@Override
	public Integer getPatientHavingPersonAttributes(PersonAttributeType attributeType, List<String> values)
	        throws APIException {
		return dao.getPatientHavingPersonAttributes(attributeType, values);
	}
}
