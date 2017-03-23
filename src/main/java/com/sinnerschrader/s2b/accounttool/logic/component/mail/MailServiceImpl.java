package com.sinnerschrader.s2b.accounttool.logic.component.mail;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service("mailService")
public class MailServiceImpl implements MailService {

	private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

	private static final String TEMPLATE_PASSWORDRESET_SUBJECT_URL = "mail/passwordReset.subject.txt";

	private static final String TEMPLATE_PASSWORDRESET_BODY_URL = "mail/passwordReset.body.txt";

	private static final String TEMPLATE_REQUESTACCESS_SUBJECT_URL =
			"mail/requestAccessToGroup.subject.txt";

	private static final String TEMPLATE_REQUESTACCESS_BODY_URL =
			"mail/requestAccessToGroup.body.txt";

	private static final String TEMPLATE_NOTIFY_UNMAINTAINED_USERS_SUBJECT_URL =
			"mail/unmaintainedUsers.subject.txt";

	private static final String TEMPLATE_NOTIFY_UNMAINTAINED_USERS_BODY_URL =
			"mail/unmaintainedUsers.body.txt";

	private static final String TEMPLATE_ACCOUNTCHANGE_SUBJECT_URL = "mail/accountChange.subject.txt";

	private static final String TEMPLATE_ACCOUNTCHANGE_BODY_URL = "mail/accountChange.body.txt";

	@Autowired
	private PebbleEngine pebbleEngine;

	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private Environment environment;

	@Value("${domain.public}")
	private String publicDomain;

	@Value("${spring.mail.from}")
	private String from;

	@Value("${spring.mail.reply}")
	private String reply;

	@Value("${spring.mail.logOnly}")
	private boolean logOnly;

	public boolean sendMailForAccountChange(User currentUser, String action) {
		final Map<String, Object> params = new LinkedHashMap<>();
		params.put("profile", currentUser);
		params.put("action", action);
		params.put("publicDomain", publicDomain);
		return sendMail(
				Collections.singletonList(currentUser),
				TEMPLATE_ACCOUNTCHANGE_SUBJECT_URL,
				TEMPLATE_ACCOUNTCHANGE_BODY_URL,
				params);
	}

	public boolean sendMailForPasswordReset(
			LdapUserDetails currentUser, User user, String newPassword) {
		final Map<String, Object> params = new LinkedHashMap<>();
		params.put("currentUser", currentUser);
		params.put("user", user);
		params.put("password", newPassword);
		params.put("publicDomain", publicDomain);

		return sendMail(
				Collections.singletonList(user),
				TEMPLATE_PASSWORDRESET_SUBJECT_URL,
				TEMPLATE_PASSWORDRESET_BODY_URL,
				params);
	}

	public boolean sendMailForRequestAccessToGroup(
			LdapUserDetails currentUser, List<User> receipients, Group adminGroup, Group wishGroup) {
		final Map<String, Object> params = new LinkedHashMap<>();
		params.put("currentUser", currentUser);
		params.put("requestedGroup", wishGroup);
		params.put("adminGroup", adminGroup);
		params.put("publicDomain", publicDomain);

		return sendMail(
				receipients, TEMPLATE_REQUESTACCESS_SUBJECT_URL, TEMPLATE_REQUESTACCESS_BODY_URL, params);
	}

	@Override
	public boolean sendNotificationOnUnmaintainedAccounts(
			String[] receipients, Map<String, List<User>> unmaintainedUsers) {
		final Map<String, Object> params = new LinkedHashMap<>();
		params.putAll(unmaintainedUsers);
		params.put("publicDomain", publicDomain);
		return sendMail(
				receipients,
				TEMPLATE_NOTIFY_UNMAINTAINED_USERS_SUBJECT_URL,
				TEMPLATE_NOTIFY_UNMAINTAINED_USERS_BODY_URL,
				params);
	}

	private String loadTemplate(String templateUrl, Map<String, Object> params) throws IOException {
		try {
			StringWriter writer = new StringWriter();
			PebbleTemplate template = pebbleEngine.getTemplate(templateUrl);
			template.evaluate(writer, params, Locale.ENGLISH);
			return writer.toString();
		} catch (PebbleException pe) {
			log.error("Could not process template", pe);
			throw new IOException("Could not load template", pe);
		}
	}

	private boolean sendMail(
			List<User> receipients,
			String subjectTemplateUrl,
			String bodyTemplateUrl,
			Map<String, Object> templateModels) {
		final String[] to =
				receipients
						.stream()
						.map(User::getMail)
						.filter(StringUtils::isNotBlank)
						.collect(Collectors.toList())
						.toArray(new String[0]);
		return sendMail(to, subjectTemplateUrl, bodyTemplateUrl, templateModels);
	}

	private boolean sendMail(
			String[] to,
			String subjectTemplateUrl,
			String bodyTemplateUrl,
			Map<String, Object> templateModels) {
		final String messageID = UUID.randomUUID().toString();
		Map<String, Object> context = new LinkedHashMap<>(templateModels);
		context.put("messageID", messageID);

		log.debug("[MessageID:{}] Preparing email", messageID);
		try {
			final String subject = loadTemplate(subjectTemplateUrl, context);
			final String body = loadTemplate(bodyTemplateUrl, context);

			if (log.isDebugEnabled() || logOnly) {
				log.info("[MessageID:{}] FROM: {}", messageID, from);
				log.info("[MessageID:{}] REPLY-TO: {}", messageID, reply);
				log.info("[MessageID:{}] TO: {}", messageID, Arrays.toString(to));
				log.info("[MessageID:{}] SUBJECT: {}", messageID, subject);
				log.info("[MessageID:{}] BODY: {}", messageID, body);
				if (logOnly) {
					return true;
				}
			}

			MimeMessage mail = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mail, "UTF-8");
			helper.setFrom(from);
			helper.setReplyTo(reply);
			helper.setSubject(subject);
			helper.setText(body);
			helper.setTo(to);
			javaMailSender.send(helper.getMimeMessage());
			log.info("[MessageID:{}] E-Mail was successfully sent", messageID);
			return true;
		} catch (Exception e) {
			log.error("Could not sent mail", e);
			return false;
		}
	}
}
