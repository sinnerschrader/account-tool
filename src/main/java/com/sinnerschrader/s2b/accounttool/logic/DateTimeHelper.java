package com.sinnerschrader.s2b.accounttool.logic;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Helper Utils class for converting legacy Date objects into new Java Time API Objects and versa.
 */
public class DateTimeHelper {

	private static final Logger log = LoggerFactory.getLogger(DateTimeHelper.class);

	public static Date toDate(LocalDateTime dateTime) {
		return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date toDate(LocalDate date) {
		return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	public static String toDateString(LocalDate date, String pattern) {
		return DateTimeFormatter.ofPattern(pattern).format(date);
	}

	public static Date toDate(String date, String... pattern) {
		try {
			return DateUtils.parseDate(date, pattern);
		} catch (ParseException pe) {
			log.warn("Could not parse date string {} with pattern {}", date, pattern);
			if (log.isDebugEnabled()) {
				log.error("Could not parse date " + date, pe);
			}
			return null;
		}
	}

	public static LocalDateTime toLocalDateTime(Date date) {
		return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	}

	public static LocalDate toLocalDate(Date date) {
		return LocalDate.from(date.toInstant());
	}

	public static String getDurationString(LocalDateTime start, LocalDateTime end, ChronoUnit unit) {
		return unit.between(start, end) + " " + unit.name();
	}
}
