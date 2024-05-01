/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.outcomes.api.dao;

import org.junit.Test;
import org.junit.Ignore;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.outcomes.Questionaire;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * It is an integration test (extends BaseModuleContextSensitiveTest), which verifies DAO methods
 * against the in-memory H2 database. The database is initially loaded with data from
 * standardTestDataset.xml in openmrs-api. All test methods are executed in transactions, which are
 * rolled back by the end of each test method.
 */
public class OutcomesDaoTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	OutcomesDao dao;
	
	@Autowired
	UserService userService;
	
	@Test
	@Ignore("Unignore if you want to make the OutcomesQuestionnaire class persistable, see also OutcomesQuestionnaire and liquibase.xml")
	public void saveItem_shouldSaveAllPropertiesInDb() {
		//Given
		Questionaire questionaire = new Questionaire();
		questionaire.setDescription("some description");
		questionaire.setPollster(userService.getUser(1));
		//When
		dao.saveQuestionnaire(questionaire);
		//Let's clean up the cache to be sure getQuestionnaireByUuid fetches from DB and not from cache
		Context.flushSession();
		Context.clearSession();
		//Then
		Questionaire savedQuestionaire = dao.getQuestionnaireByUuid(questionaire.getUuid());
		assertThat(savedQuestionaire, hasProperty("uuid", is(savedQuestionaire.getUuid())));
		assertThat(savedQuestionaire, hasProperty("pollster", is(savedQuestionaire.getPollster())));
		assertThat(savedQuestionaire, hasProperty("description", is(savedQuestionaire.getDescription())));
	}
}
