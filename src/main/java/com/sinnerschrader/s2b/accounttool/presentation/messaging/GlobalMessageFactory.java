package com.sinnerschrader.s2b.accounttool.presentation.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Factory to create GlobalMessages
 */
@Component(value = "globalMessageFactory")
public class GlobalMessageFactory {

	private static final String SESSION_KEY = GlobalMessageFactory.class.getName();

	@Autowired
	private MessageSource messageSource;

	public GlobalMessage create(GlobalMessageType type, String messageKey, String... args) {
		return new GlobalMessage(
				messageKey, messageSource.getMessage(messageKey, args, Locale.ENGLISH), type);
	}

	public GlobalMessage createError(String messageKey, String... args) {
		return create(GlobalMessageType.ERROR, messageKey, args);
	}

	public GlobalMessage createInfo(String messageKey, String... args) {
		return create(GlobalMessageType.INFO, messageKey, args);
	}

	@SuppressWarnings("unchecked")
	public void store(HttpServletRequest request, GlobalMessage message) {
		HttpSession session = request.getSession();
		if (session.getAttribute(SESSION_KEY) == null) {
			session.setAttribute(SESSION_KEY, new LinkedList<GlobalMessage>());
		}
		List<GlobalMessage> messages = (List<GlobalMessage>) session.getAttribute(SESSION_KEY);
		messages.add(message);
	}

	@SuppressWarnings("unchecked")
	public List<GlobalMessage> pop(HttpServletRequest request) {
		HttpSession session = request.getSession();
		if (session.getAttribute(SESSION_KEY) == null) {
			return new ArrayList<>();
		}
		List<GlobalMessage> messages = (List<GlobalMessage>) session.getAttribute(SESSION_KEY);
		session.removeAttribute(SESSION_KEY);
		return messages;
	}
}
