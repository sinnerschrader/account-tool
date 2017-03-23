package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.DateTimeHelper;
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** */
public class LdapBusinessServiceImpl implements LdapBusinessService, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(LdapBusinessServiceImpl.class);

	private static final String NEAR_FUTURE_EXITING = "leavingUsers";

	private static final String EXITED_ACTIVE_USERS = "unmaintainedUsers";

	private static final String ACTIVE_MAIL_ON_INACTIVE_USER = "unmaintainedMailUsers";

	private Cache<String, List<User>> userCache = null;

	@Autowired
	private LdapService ldapService;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private LdapManagementConfiguration managementConfiguration;

	@Autowired
	private MailService mailService;

	protected LDAPConnection createManagementConnection() throws LDAPException {
		LDAPConnection connection = null;
		try {
			connection = ldapConfiguration.createConnection();
			connection.bind(
					managementConfiguration.getUser().getBindDN(),
					managementConfiguration.getUser().getPassword());
		} catch (GeneralSecurityException e) {
			log.error("Could not open a management connection to ldap", e);
		}
		return connection;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		userCache = CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();
	}

	@Scheduled(cron = "${ldap-management.jobs.updateUnmaintained.cronExpr}")
	protected void updateUnmaintained() {
		if (!managementConfiguration.getJobs().getUpdateUnmaintained().isActive()
				|| !managementConfiguration.getJobs().isActive()) {
			return;
		}

		log.debug("Updating the informations about unmaintained accounts");
		LocalDateTime startTime = LocalDateTime.now();
		LDAPConnection connection = null;
		final int nextWeeks = managementConfiguration.getLeavingUsersInCW();
		List<User> exitedActiveUsers = new LinkedList<>();
		List<User> futureExitingUser = new LinkedList<>();
		List<User> activeMailAccounts = new LinkedList<>();
		try {
			connection = createManagementConnection();
			final int userCount = ldapService.getUserCount(connection);
			final int blockSize = 250;
			List<User> users = null;
			LocalDate today = LocalDate.now();
			LocalDate nearFuture = today.plusWeeks(nextWeeks);
			for (int i = 0; i < userCount; i = i + blockSize) {
				users = ldapService.getUsers(connection, i, blockSize);
				if (users != null && !users.isEmpty()) {
					for (User user : users) {
						LocalDate exitDate = user.getEmployeeExitDate();
						if (exitDate != null && user.getSzzStatus() == User.State.active) {
							if (exitDate.isBefore(today)) {
								exitedActiveUsers.add(user);
							} else if (exitDate.isBefore(nearFuture)) {
								futureExitingUser.add(user);
							}
						}
						if (user.getSzzStatus() == User.State.inactive
								&& user.getSzzMailStatus() == User.State.active) {
							activeMailAccounts.add(user);
						}
					}
				}
			}
			log.debug("Found {} user who are exited but have active accounts", exitedActiveUsers.size());
			log.debug(
					"Found {} user who are leaving in the next {} weeks",
					futureExitingUser.size(),
					nextWeeks);
			log.debug("Found {} active mail addresses on inactive accounts", activeMailAccounts.size());
			userCache.put(EXITED_ACTIVE_USERS, exitedActiveUsers);
			userCache.put(NEAR_FUTURE_EXITING, futureExitingUser);
			userCache.put(ACTIVE_MAIL_ON_INACTIVE_USER, activeMailAccounts);

			log.info(
					"Updated the informations about unmaintained accounts in {}",
					DateTimeHelper.getDurationString(startTime, LocalDateTime.now(), ChronoUnit.MILLIS));
		} catch (LDAPException e) {
			log.error("Could not update unmaintained users", e);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	@Scheduled(cron = "${ldap-management.jobs.notifyAboutUnmaintained.cronExpr}")
	protected void notifyAboutUnmaintained() {
		if (!managementConfiguration.getJobs().getNotifyAboutUnmaintained().isActive()
				|| !managementConfiguration.getJobs().isActive()) {
			return;
		}

		if (userCache.size() > 0) {
			final String[] to = managementConfiguration.getNotifyReceipients().toArray(new String[0]);
			mailService.sendNotificationOnUnmaintainedAccounts(to, userCache.asMap());
		}
	}

	@Override
	public List<User> getUnmaintainedMailUsers() {
		List<User> res = userCache.getIfPresent(ACTIVE_MAIL_ON_INACTIVE_USER);
		if (res == null) {
			res = Collections.emptyList();
		}
		return res;
	}

	@Override
	public List<User> getUnmaintainedExternals() {
		List<User> res = userCache.getIfPresent(EXITED_ACTIVE_USERS);
		if (res == null) {
			res = Collections.emptyList();
		}
		return res;
	}

	@Override
	public List<User> getLeavingUsers() {
		List<User> res = userCache.getIfPresent(NEAR_FUTURE_EXITING);
		if (res == null) {
			res = Collections.emptyList();
		}
		return res;
	}

	@Override
	public void addDefaultGroups(User user) {
		LDAPConnection connection = null;
		List<String> defaultGroups = ldapConfiguration.getDefaultGroups();
		if (defaultGroups == null || defaultGroups.isEmpty()) {
			log.debug("No default groups defined, skipped adding user to default groups");
			return;
		}
		try {
			connection = createManagementConnection();
			for (String groupCn : defaultGroups) {
				Group group = ldapService.getGroupByCN(connection, groupCn);
				if (group != null) {
					ldapService.addUserToGroup(connection, user, group);
				}
			}
			log.debug("Added user to {} default groups", defaultGroups.size());
		} catch (LDAPException le) {
			log.error("Could not add user to default groups", le);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	@Override
	public void delDefaulGroups(User user) {
		LDAPConnection connection = null;
		List<String> defaultGroups = ldapConfiguration.getDefaultGroups();
		if (defaultGroups == null || defaultGroups.isEmpty()) {
			log.debug("No default groups defined, skipped removing user from default groups");
			return;
		}
		try {
			connection = createManagementConnection();
			for (String groupCn : defaultGroups) {
				Group group = ldapService.getGroupByCN(connection, groupCn);
				if (group != null) {
					ldapService.removeUserFromGroup(connection, user, group);
				}
			}
			log.debug("Removed user from {} default groups", defaultGroups.size());
		} catch (LDAPException le) {
			log.error("Could not remove user from default groups", le);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}
}
