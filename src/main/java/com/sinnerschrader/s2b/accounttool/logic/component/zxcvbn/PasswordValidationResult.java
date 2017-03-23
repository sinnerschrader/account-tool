package com.sinnerschrader.s2b.accounttool.logic.component.zxcvbn;

import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/** */
public class PasswordValidationResult {

	private final boolean valid;

	private final int score;

	private final String messageCode;

	private final List<String> suggestionCodes;

	private PasswordValidationResult(int score, String messageCode, List<String> suggestionCodes) {
		this.valid = score >= 3;
		this.suggestionCodes =
				(suggestionCodes != null)
						? Collections.unmodifiableList(suggestionCodes)
						: Collections.emptyList();
		this.score = score;
		this.messageCode = messageCode;
	}

	PasswordValidationResult(Strength strength, Feedback feedback) {
		this(strength.getScore(), feedback.getWarning(), feedback.getSuggestions());
	}

	public List<String> getErrorCodes() {
		List<String> codes = new LinkedList<>();
		if (StringUtils.isNotBlank(messageCode)) {
			codes.add(messageCode);
		}
		if (!suggestionCodes.isEmpty()) {
			codes.addAll(suggestionCodes);
		}
		return codes;
	}

	public List<String> getSuggestionCodes() {
		return suggestionCodes;
	}

	public boolean isValid() {
		return valid;
	}

	public int getScore() {
		return score;
	}

	public String getMessageCode() {
		return messageCode;
	}
}
