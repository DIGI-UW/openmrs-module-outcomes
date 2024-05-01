/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.outcomes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.module.outcomes.utils.UuidGenerator;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity(name = "outcomes.Questionaire")
@Table(name = "outcomes_questionaire")
public class Questionaire extends BaseOpenmrsData {
	@Id
	@GeneratedValue
	@Column(name = "questionaire_id")
	private Integer id;

	@Column(name = "uuid")
	private final String uuid = UuidGenerator.getNextUuid();
	
	@ManyToOne
	@JoinColumn(name = "pollster")
	private User pollster;

	@ManyToOne
	@JoinColumn(name = "respondent")
	private Person respondent;
	
	@Basic
	@Column(name = "description")
	private String description;
	
	@ElementCollection
	@CollectionTable(name="outcome_questions")
	@MapKeyColumn(name="questionaire_item_type")
	@Column(name="questionaire_questions")
	Map<String, String> questions = new HashMap<>();
}
