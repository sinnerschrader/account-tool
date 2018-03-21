package com.sinnerschrader.s2b.accounttool.logic

import org.apache.commons.lang3.time.DateUtils
import org.slf4j.LoggerFactory
import java.text.ParseException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


/**
 * Helper Utils class for converting legacy Date objects into new Java Time API Objects and versa.
 */
object DateTimeHelper {
    private val LOG = LoggerFactory.getLogger(DateTimeHelper::class.java)

    fun toDate(dateTime: LocalDateTime) = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())!!
    fun toDate(date: LocalDate) = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())!!
    fun toDateString(date: LocalDate, pattern: String) = DateTimeFormatter.ofPattern(pattern).format(date)!!

    fun toDate(date: String, vararg pattern: String) =
            try {
                DateUtils.parseDate(date, *pattern)
            } catch (pe: ParseException) {
                LOG.warn("Could not parse date string {} with pattern {}", date, pattern)
                null
            }

    fun toLocalDateTime(date: Date) = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())!!
    fun toLocalDate(date: Date) = LocalDate.from(date.toInstant())!!
    fun getDurationString(start: LocalDateTime, end: LocalDateTime, unit: ChronoUnit) = unit.between(start, end).toString() + " " + unit.name
}
