package com.sinnerschrader.s2b.accounttool.logic.component.authorization;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
		code = HttpStatus.NOT_FOUND,
		reason = "Page not found",
		value = HttpStatus.NOT_FOUND
)
public class UnauthorizedException extends AccessDeniedException {

	private final String uid;

	private final String action;

	private final String reason;

	public UnauthorizedException(String uid, String action, String reason) {
		super(createMessage(uid, action, reason));
		this.uid = uid;
		this.action = action;
		this.reason = reason;
	}

	public UnauthorizedException(String uid, String action, String reason, Throwable t) {
		super(createMessage(uid, action, reason), t);
		this.uid = uid;
		this.action = action;
		this.reason = reason;
	}

	private static String createMessage(String uid, String action, String reason) {
		return "User " + uid + " is not authorized to perform " + action + ". Reason: " + reason;
	}

	public String getUid() {
		return uid;
	}

	public String getAction() {
		return action;
	}

	public String getReason() {
		return reason;
	}
}
