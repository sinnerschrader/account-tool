package com.sinnerschrader.s2b.accounttool.presentation.validation;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.component.zxcvbn.PasswordAnalyzeService;
import com.sinnerschrader.s2b.accounttool.logic.component.zxcvbn.PasswordValidationResult;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import com.sinnerschrader.s2b.accounttool.presentation.model.ChangeProfile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/** */
@Component(value = "changeProfileFormValidator")
public class ChangeProfileFormValidator implements Validator {

	private static final Logger log = LoggerFactory.getLogger(ChangeProfileFormValidator.class);

	@Autowired
	private PasswordAnalyzeService passwordAnalyzeService;

	@Override
	public boolean supports(Class<?> clazz) {
		return ChangeProfile.class == clazz || ChangeProfile.class.isAssignableFrom(clazz);
	}

	private void validateAndSanitizeSSHKey(ChangeProfile form, Errors errors) {
		final String formAttribute = "publicKey";

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publicKey", "required");
		if (errors.hasErrors()) return;

		String sanitizedKey = "";
		final String[] parts = StringUtils.split(form.getPublicKey(), " ");
		if (parts.length >= 2) {
			try {
				String base64Key = parts[1];
				byte[] binary = Base64.decodeBase64(base64Key);
				PublicKey publicKey = new ByteArrayBuffer(binary).getRawPublicKey();
				if (StringUtils.equals(publicKey.getAlgorithm(), "EC")) {
					ECPublicKey ecKey = (ECPublicKey) publicKey;
					if (ecKey.getParams().getOrder().bitLength() < 256) {
						errors.rejectValue(
								formAttribute, "publicKey.ec.insecure", "ec key with minimum of 256 bit required");
						return;
					}
					sanitizedKey = "ecdsa-sha2-nistp" + ecKey.getParams().getOrder().bitLength() + " ";
				} else if (StringUtils.equals(publicKey.getAlgorithm(), "RSA")) {
					RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
					if (rsaPublicKey.getModulus().bitLength() < 2048) {
						errors.rejectValue(
								formAttribute,
								"publicKey.rsa.insecure",
								"rsa key with minimum of 2048 bit required");
						return;
					}
					sanitizedKey = "ssh-rsa ";
				} else if (StringUtils.equals(publicKey.getAlgorithm(), "DSA")) {
					DSAPublicKey dsaPublicKey = (DSAPublicKey) publicKey;
					if (dsaPublicKey.getParams().getP().bitLength() < 2048) {
						errors.rejectValue(
								formAttribute,
								"publicKey.dsa.insecure",
								"dsa key with minimum of 2048 bit required");
						return;
					}
					sanitizedKey = "ssh-dss ";
				}
				final Buffer buffer = new ByteArrayBuffer();
				buffer.putRawPublicKey(publicKey);
				sanitizedKey += Base64.encodeBase64String(buffer.getCompactData());
				form.setPublicKey(sanitizedKey);
			} catch (Exception e) {
				final String msg = "Could not parse public key";
				errors.rejectValue(
						formAttribute, "publicKey.invalid", "The defined public can't be parsed");
				log.error(msg);
				if (log.isDebugEnabled()) {
					log.error(msg, e);
				}
			}
		} else {
			errors.rejectValue(formAttribute, "publicKey.invalid", "The defined public can't be parsed");
		}
	}

	private void validatePassword(ChangeProfile form, Errors errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "oldPassword", "required", "Please enter your current password");
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "password", "required", "Please enter a password");
		if (errors.hasErrors()) return;
		ValidationUtils.rejectIfEmptyOrWhitespace(
				errors, "passwordRepeat", "required", "Please repeat the password");
		if (errors.hasErrors()) return;

		LdapUserDetails currentUser = RequestUtils.getCurrentUserDetails();
		if (!StringUtils.equals(currentUser.getPassword(), form.getOldPassword())) {
			errors.rejectValue("oldPassword", "password.previous.notMatch");
		}
		if (StringUtils.equals(currentUser.getPassword(), form.getPassword())) {
			errors.rejectValue("password", "password.noChange");
		}

		PasswordValidationResult result = passwordAnalyzeService.analyze(form.getPassword());
		if (!result.isValid()) {
			List<String> codes = result.getErrorCodes();
			if (codes.isEmpty()) {
				// this is a fallback, if no message and no suggestions are provided
				errors.rejectValue(
						"password", "passwordValidation.general.error", "The password is too weak");
			} else {
				for (String code : result.getErrorCodes()) {
					errors.rejectValue("password", code, "Your password violates a rule.");
				}
			}
		}
	}

	@Override
	public void validate(Object target, Errors errors) {
		ChangeProfile form = (ChangeProfile) target;
		if (form.isPublicKeyChange()) {
			validateAndSanitizeSSHKey(form, errors);
		} else if (form.isPhoneChange()) {
			//ValidationUtils.rejectIfEmptyOrWhitespace(errors, "telephone", "required",
			//	"Please enter a telephone number");
		} else if (form.isPasswordChange()) {
			validatePassword(form, errors);
		}
	}
}
