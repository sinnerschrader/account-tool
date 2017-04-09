package com.sinnerschrader.s2b.accounttool.logic.component.zxcvbn;

import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/** */
@Service("passwordAnalyzeService")
public class PasswordAnalyzeServiceImpl implements PasswordAnalyzeService, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(PasswordAnalyzeServiceImpl.class);

	private static final String DICTIONARY_PROP_PREFIX = "zxcvbn.dictionary.";

	@Value("#{'${zxcvbn.dictionary.active}'.split(',')}")
	private List<String> dictionaryRefs;

	@Autowired
	private Environment environment;

	@Autowired
	private ResourceLoader resourceLoader;

	private Zxcvbn zxcvbn;

	private List<String> dictionary;

	public PasswordAnalyzeServiceImpl() {
		zxcvbn = new Zxcvbn();
		dictionary = new LinkedList<>();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (dictionaryRefs == null) {
			throw new IllegalStateException("The List of Dictionary refs is not set");
		}
		Set<String> dictionaryFiles = new HashSet<>();
		for (String ref : dictionaryRefs) {
			ref = StringUtils.trimToEmpty(ref); // avoid spaces after seperator
			String dict = StringUtils.trimToNull(environment.getProperty(DICTIONARY_PROP_PREFIX + ref));
			if (dict != null) {
				log.debug("Found dictionary {}", dict);
				dictionaryFiles.add(dict);
			}
		}
		Set<String> result = new LinkedHashSet<>();
		for (String dictionaryFile : dictionaryFiles) {
			Resource res = resourceLoader.getResource(dictionaryFile);
			if (res.exists()) {
				Set<String> tmp = new LinkedHashSet<>();
				try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream()))) {
					String line;
					while ((line = br.readLine()) != null) {
						line = StringUtils.trimToNull(line);
						if (line != null) {
							tmp.add(line);
						}
					}
				}
				result.addAll(tmp);
				log.debug("Loaded {} entries from file {}", tmp.size(), res.getFilename());
			}
		}
		log.info("Loaded {} entries from {} file(s)", result.size(), dictionaryFiles.size());
		this.dictionary.addAll(result);
	}

	@Override
	public PasswordValidationResult analyze(String password) {
		Strength strength = zxcvbn.measure(password, dictionary);
		Feedback feedback =
				strength
						.getFeedback()
						.withResourceBundle(
								new ResourceBundle() {

									@Override
									protected Object handleGetObject(String key) {
										return key;
									}

									@Override
									public Enumeration<String> getKeys() {
										return null;
									}
								});
		return new PasswordValidationResult(strength, feedback);
	}
}
