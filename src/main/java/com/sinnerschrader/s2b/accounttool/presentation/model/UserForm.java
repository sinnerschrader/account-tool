package com.sinnerschrader.s2b.accounttool.presentation.model;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.DateTimeHelper;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** */
public class UserForm implements Serializable {

	private static final String BIRTHDAY_PATTERN = "dd.MM";

	private static final String DATE_PATTERN = "dd.MM.yyyy";

	private String save;

	private String deactivateUser;

	private String activateUser;

	private String resetPassword;

	private String uid;

	private String employeeNumber;

	private String firstName;

	private String lastName;

	private String email;

	private String company;

	private String location;

	private String title;

	private String type;

	private String team;

	private String telephoneNumber;

	private String mobileNumber;

	private String status;

	private String mailStatus;

	/**
	 * Only Date and Month (Format: dd.MM)
	 */
	private String birthDate;

	/**
	 * Entry date (Format: dd.MM.yyyy)
	 */
	private String entryDate;

	/**
	 * Entry date (Format: dd.MM.yyyy)
	 */
	private String exitDate;

	public UserForm() {
		this.uid = "";
		this.employeeNumber = "";
		this.firstName = "";
		this.lastName = "";
		this.company = "";
		this.location = "";
		this.title = "";
		this.email = "";
		this.type = "Mitarbeiter";
		this.team = "";
		this.telephoneNumber = "";
		this.mobileNumber = "";
		this.status = "active";
		this.mailStatus = "active";
		this.birthDate = "";

		LocalDate entry = LocalDate.now().plusMonths(1).withDayOfMonth(1);
		this.entryDate = DateTimeHelper.toDateString(entry, DATE_PATTERN);
		this.exitDate = DateTimeHelper.toDateString(entry.plusYears(50L).minusDays(1), DATE_PATTERN);
	}

	public UserForm(LdapUserDetails details) {
		this();
		if (details != null) {
			this.company = details.getCompany();
		}
	}

	public UserForm(User user) {
		this();

		this.uid = user.getUid();
		this.employeeNumber = user.getEmployeeNumber();
		this.firstName = user.getGivenName();
		this.lastName = user.getSn();
		this.company = user.getCompanyKey();
		this.location = user.getL();
		this.title = user.getTitle();
		this.type = user.getDescription();
		this.team = user.getOu();
		this.telephoneNumber = user.getTelephoneNumber();
		this.mobileNumber = user.getMobile();
		this.status = user.getSzzStatus().name();
		this.mailStatus = user.getSzzMailStatus().name();
		this.email = user.getMail();

		if (user.getBirthDate() != null) {
			this.birthDate = DateTimeHelper.toDateString(user.getBirthDate(), BIRTHDAY_PATTERN);
		}
		if (user.getEmployeeEntryDate() != null) {
			this.entryDate = DateTimeHelper.toDateString(user.getEmployeeEntryDate(), DATE_PATTERN);
		}
		if (user.getEmployeeExitDate() != null) {
			this.exitDate = DateTimeHelper.toDateString(user.getEmployeeExitDate(), DATE_PATTERN);
		}
	}

	public LocalDate getExitAsDate() {
		return LocalDate.parse(exitDate, DateTimeFormatter.ofPattern(DATE_PATTERN));
	}

	public LocalDate getBirthAsDate() {
		// the year is not stored, but is a leap year for reasons
		if (StringUtils.isNotBlank(birthDate)) {
			return LocalDate.parse(birthDate + ".1972", DateTimeFormatter.ofPattern(DATE_PATTERN));
		}
		return null;
	}

	public LocalDate getEntryAsDate() {
		return LocalDate.parse(entryDate, DateTimeFormatter.ofPattern(DATE_PATTERN));
	}

	public User createUserEntityFromForm(LdapConfiguration ldapConfiguration) {
		LocalDate birth = getBirthAsDate();
		LocalDate entry = getEntryAsDate();
		LocalDate exit = getExitAsDate();

		return new User(
				null,
				uid,
				null,
				null,
				firstName + " " + lastName,
				firstName + " " + lastName,
				firstName + " " + lastName,
				firstName,
				lastName,
				null,
				null,
				birth,
				null,
				null,
				null,
				email,
				User.State.fromString(status),
				User.State.fromString(mailStatus),
				null,
				entry,
				exit,
				team,
				type,
				telephoneNumber,
				mobileNumber,
				employeeNumber,
				title,
				location,
				null,
				ldapConfiguration.getCompaniesAsMap().get(company),
				company);
	}

	public boolean isChangeUser() {
		return StringUtils.isNotBlank(save);
	}

	public boolean isDeactivateUser() {
		return StringUtils.isNotBlank(deactivateUser);
	}

	public boolean isActivateUser() {
		return StringUtils.isNotBlank(activateUser);
	}

	public boolean isResetpassword() {
		return StringUtils.isNotBlank(resetPassword);
	}

	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}

	public String getActivateUser() {
		return activateUser;
	}

	public void setActivateUser(String activateUser) {
		this.activateUser = activateUser;
	}

	public String getDeactivateUser() {
		return deactivateUser;
	}

	public void setDeactivateUser(String deactivateUser) {
		this.deactivateUser = deactivateUser;
	}

	public String getResetPassword() {
		return resetPassword;
	}

	public void setResetPassword(String resetPassword) {
		this.resetPassword = resetPassword;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getTelephoneNumber() {
		return telephoneNumber;
	}

	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMailStatus() {
		return mailStatus;
	}

	public void setMailStatus(String mailStatus) {
		this.mailStatus = mailStatus;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public String getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(String entryDate) {
		this.entryDate = entryDate;
	}

	public String getExitDate() {
		return exitDate;
	}

	public void setExitDate(String exitDate) {
		this.exitDate = exitDate;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
