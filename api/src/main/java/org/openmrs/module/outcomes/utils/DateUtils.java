package org.openmrs.module.outcomes.utils;

import org.joda.time.DateTime;

import java.util.Date;

public class DateUtils {
	
	public static Date getStartOfDay(Date startDate) {
		return new DateTime(startDate).withTimeAtStartOfDay().toDate();
	}
	
	public static Date getEndOfDay(Date endDate) {
		return new DateTime(endDate).withTime(23, 59, 59, 999).toDate();
	}
}
