/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.outcomes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Getter;
import lombok.Setter;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.module.outcomes.utils.UuidGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "outcomes.Questionaire")
@Table(name = "outcomes_questionaire")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Questionaire extends BaseOpenmrsData {
	
	@Id
	@GeneratedValue
	@Column(name = "questionaire_id")
	public Integer id;
	
	@Column(name = "uuid")
	public final String uuid = UuidGenerator.getNextUuid();
	
	@Setter
	@Getter
	@Column(name = "resource")
	public String resource;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Override
	public String getUuid() {
		return uuid;
	}
	
	@Override
	public String toString() {
		return "org.openmrs.module.outcomes.Questionaire{" + "creator=" + creator + ", id=" + id + ", uuid='" + uuid + '\''
		        + ", resource=" + resource + ", creator=" + getCreator() + ", dateCreated=" + getDateCreated() + ", voided="
		        + getVoided() + ", dateVoided=" + getDateVoided() + ", voidedBy=" + getVoidedBy() + ", voidReason='"
		        + getVoidReason() + '\'' + '}';
	}
}
