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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.outcomes.Questionaire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("outcomes.OutcomesDao")
public class OutcomesDao {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	public Questionaire getQuestionnaireByUuid(String uuid) {
		return (Questionaire) getSession().createCriteria(Questionaire.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public Questionaire saveQuestionnaire(Questionaire questionaire) {
		getSession().saveOrUpdate(questionaire);
		return questionaire;
	}
	
	public void deleteQuestionnaire(Questionaire questionaire) {
		getSession().delete(questionaire);
	}
	
	public Integer getPatientHavingPersonAttributes(PersonAttributeType attributeType, List<String> values) {
		StringBuilder sqlQuery = new StringBuilder();
		sqlQuery.append("SELECT patient.patient_id ");
		sqlQuery.append("FROM person_attribute ");
		sqlQuery.append("INNER JOIN patient ON patient.patient_id = person_attribute.person_id ");
		sqlQuery.append("INNER JOIN person ON person.person_id = person_attribute.person_id ");
		sqlQuery.append("WHERE person_attribute.voided = false ");
		sqlQuery.append("AND person.voided = false ");
		sqlQuery.append("AND patient.voided = false ");
		if (attributeType != null)
			sqlQuery.append(" AND person_attribute.person_attribute_type_id = :attributeType ");
		
		if (values != null && !values.isEmpty()) {
			if (values.size() == 1) {
				sqlQuery.append(" AND person_attribute.value = :value");
			} else {
				sqlQuery.append(" AND person_attribute.value in (:values) ");
			}
		}
		sqlQuery.append(" GROUP BY patient.patient_id ");
		log.debug("query: " + sqlQuery);
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sqlQuery.toString());
		if (attributeType != null)
			query.setInteger("attributeType", attributeType.getPersonAttributeTypeId());
		
		if (values != null && !values.isEmpty()) {
			if (values.size() == 1) {
				query.setString("value", values.get(0));
			} else if (!values.isEmpty()) {
				query.setParameterList("values", values);
			}
		}
		return (Integer) query.uniqueResult();
	}
}
