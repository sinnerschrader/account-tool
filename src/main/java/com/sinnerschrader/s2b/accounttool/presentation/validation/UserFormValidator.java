package com.sinnerschrader.s2b.accounttool.presentation.validation;

import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.presentation.model.UserForm;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/** */
@Component(value = "userFormValidator")
public class UserFormValidator implements Validator {

	private static final Logger log = LoggerFactory.getLogger(UserFormValidator.class);

	private final EmailValidator emailValidator = new EmailValidator();

	@Value("${domain.primary}")
	private String primaryDomain;

	@Value("${domain.secondary}")
	private String secondaryDomain;

	public String getPrimaryDomain() {
		return primaryDomain;
	}

	public String getSecondaryDomain() {
		return secondaryDomain;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz == UserForm.class || UserForm.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		// simple required checks
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "firstName", "required", "Firstname is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "lastName", "required", "Lastname is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "company", "required", "Company is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "location", "required", "Location is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "title", "required", "Title is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "type", "required", "type required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "team", "required", "Team is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "status", "required", "Status is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "mailStatus", "required", "E-Mail Status is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "entryDate", "required", "Entry date is required");
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "exitDate", "required", "Exit date is required");
		if (errors.hasErrors()) {
			return;
		}

		// some formatting checks on email and dates
		UserForm form = (UserForm) target;
		if (StringUtils.isNotBlank(form.getUid())) {
			String uid = StringUtils.trimToEmpty(form.getUid());
			if (uid.length() < 6 || uid.length() > 8 || !StringUtils.isAlpha(uid)) {
				errors.rejectValue(
						"uid",
						"domain",
						new Object[]{form.getUid()},
						"Please enter a valid username with six, seven or eigth characters");
			}
		}

		final String requiredPriDomain = "@" + primaryDomain;
		final String requiredSecDomain = "@" + secondaryDomain;
		if (StringUtils.isNotBlank(form.getEmail())) {
			String email = StringUtils.trimToEmpty(form.getEmail());
			if (!emailValidator.isValid(email, null)) {
				errors.rejectValue(
						"email", "email", new Object[]{form.getEmail()}, "Entered E-Mail is not valid");
			}
			if (!StringUtils.endsWith(email, requiredPriDomain)
					&& !StringUtils.endsWith(email, requiredSecDomain)) {
				errors.rejectValue(
						"email",
						"domain",
						new Object[]{form.getEmail()},
						"E-Mail has to be part of " + requiredPriDomain + " Domain");
			}
		}

		if (User.State.fromString(form.getStatus()) == User.State.undefined) {
			errors.rejectValue(
					"status", "invalid.status", new Object[]{}, "Status has an invalid value");
		}
		if (User.State.fromString(form.getMailStatus()) == User.State.undefined) {
			errors.rejectValue(
					"mailStatus", "invalid.status", new Object[]{}, "E-Mail Status has an invalid value");
		}

		if (StringUtils.isNotBlank(form.getBirthDate())) {
			try {
				form.getBirthAsDate().getYear();
			} catch (Exception e) {
				errors.rejectValue(
						"birthDate",
						"date",
						new Object[]{form.getBirthDate()},
						"Birth Date is not a valid date.");
			}
		}

		try {
			form.getEntryAsDate().getYear();
		} catch (Exception e) {
			errors.rejectValue(
					"entryDate",
					"date",
					new Object[]{form.getEntryDate()},
					"Entry Date is not a valid date.");
		}
		try {
			form.getExitAsDate().getYear();
		} catch (Exception e) {
			errors.rejectValue(
					"exitDate", "date", new Object[]{form.getExitDate()}, "Exit Date is not a valid date.");
		}
	}
}
