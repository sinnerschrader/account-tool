package com.sinnerschrader.s2b.accounttool.presentation.messaging;

import java.io.Serializable;

/**
 * A Message object for displaying global messages.
 */
public class GlobalMessage implements Serializable {

	private final String key;

	private final String text;

	private final GlobalMessageType type;

	GlobalMessage(String key, String text, GlobalMessageType type) {
		this.key = key;
		this.text = text;
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public String getText() {
		return text;
	}

	public GlobalMessageType getType() {
		return type;
	}
}
