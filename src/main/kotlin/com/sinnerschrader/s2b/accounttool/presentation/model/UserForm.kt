package com.sinnerschrader.s2b.accounttool.presentation.model

import com.sinnerschrader.s2b.accounttool.config.DomainConfiguration
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.DateTimeHelper
import com.sinnerschrader.s2b.accounttool.logic.entity.User

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class UserForm(
    var save: String = "",
    var deactivateUser: String = "",
    var activateUser: String = "",
    var resetPassword: String = "",

    var uid: String = "",
    var employeeNumber: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var emailPrefix: String = "",
    val emailDomain: String = "",
    var company: String = "",
    var location: String = "",
    var title: String = "",
    var type: String = "",
    var team: String = "",
    var telephoneNumber: String = "",
    var mobileNumber: String = "",
    var status: Boolean = true,
    var mailStatus: Boolean = true,
    var birthDate: String = "",
    var entryDate: String =  LocalDate.now().plusMonths(1).withDayOfMonth(1).format(DATE_PATTERN),
    var exitDate: String = LocalDate.now().plusMonths(1).withDayOfMonth(1).plusYears(50L).minusDays(1).format(DATE_PATTERN)
) {

    companion object {
        const val BIRTHDAY_PATTERN = "dd.MM"
        const val DATE_PATTERN = "dd.MM.yyyy"
    }

    constructor(details: LdapUserDetails) : this(company = details.company)

    constructor(user: User) : this(
        uid = user.uid,
        employeeNumber = user.employeeNumber,
        firstName = user.givenName,
        lastName = user.sn,
        emailPrefix = user.mail.substringBefore('@'),
        emailDomain = user.mail.substringAfter('@'),
        company = user.companyKey,
        location = user.l,
        title = user.title,
        type = user.description,
        team = user.ou,
        telephoneNumber = user.telephoneNumber,
        mobileNumber = user.mobile,
        status = user.szzStatus == User.State.active,
        mailStatus = user.szzMailStatus == User.State.active
        ) {
        this.birthDate = if(user.szzBirthDay != -1 && user.szzBirthMonth != -1) "${user.szzBirthDay.toString().padStart(2, '0')}.${user.szzBirthMonth.toString().padStart(2,'0')}" else ""
        user.szzEntryDate?.let { this.entryDate = it.format(DATE_PATTERN)}
        user.szzExitDate?.let { this.exitDate = it.format(DATE_PATTERN) }
    }

    fun birthAsDate() = if (birthDate.isNotBlank()) "$birthDate.1972".parseLocalDate(DATE_PATTERN) else null
    fun entryAsDate() = entryDate.parseLocalDate(DATE_PATTERN)
    fun exitAsDate() = exitDate.parseLocalDate(DATE_PATTERN)

    fun createUserEntityFromForm(ldapConfiguration: LdapConfiguration, domainConfiguration: DomainConfiguration, user: User = User(uid = uid, givenName = firstName, sn = lastName)) = user.copy(
            szzBirthDay = birthDate.split(".").getOrNull(0)?.toIntOrNull() ?: -1,
            szzBirthMonth = birthDate.split(".").getOrNull(1)?.toIntOrNull() ?: -1,
            mail = when {
                emailPrefix.isNotBlank() ->  domainConfiguration.mailDomain(type)
                else ->  ""
            },
            szzStatus = if(status) User.State.active else User.State.inactive,
            szzMailStatus = if(mailStatus) User.State.active else User.State.inactive,
            sambaPwdLastSet = Long.MAX_VALUE,
            szzEntryDate = entryAsDate(),
            szzExitDate = exitAsDate(),
            ou = team,
            description = type,
            telephoneNumber = telephoneNumber,
            mobile = mobileNumber,
            employeeNumber = employeeNumber,
            title = title,
            l = location,
            o = ldapConfiguration.companies[company]!!,
            companyKey = company)
}

private fun LocalDate.format(pattern: String) = DateTimeHelper.toDateString(this, pattern)
private fun String.parseLocalDate(pattern: String) = LocalDate.parse(this, DateTimeFormatter.ofPattern(pattern))!!