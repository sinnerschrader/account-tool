package com.sinnerschrader.s2b.accounttool.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** */
@Component
public class LogService {

	private static final Logger log = LoggerFactory.getLogger(LogService.class);

	@Autowired
	private MessageSource messageSource = null;

	@Value("${logService.simple}")
	private boolean simple = false;

	public void event(String key, String... args) {
		String eventMsg = messageSource.getMessage(key, args, Locale.ENGLISH);
		if (simple) {
			System.out.println(eventMsg);
		} else {
			log.info(eventMsg);
		}
	}
}
