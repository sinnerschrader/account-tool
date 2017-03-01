package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.DateTimeHelper;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 *
 */
@Component
public class LdapBusinessServiceImpl implements LdapBusinessService, InitializingBean
{

	private final static Logger log = LoggerFactory.getLogger(LdapBusinessServiceImpl.class);

	private final static String NEAR_FUTURE_EXITING = "nearFutureExiting";

	private final static String EXITED_ACTIVE_USERS = "exitedActiveUsers";

	private Cache<String, List<User>> userCache = null;

	@Autowired
	private LdapService ldapService;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private LdapManagementConfiguration managementConfiguration;

	protected LDAPConnection createManagementConnection() throws LDAPException
	{
		LDAPConnection connection = null;
		try
		{
			connection = ldapConfiguration.createConnection();
			connection.bind(managementConfiguration.getUser().getBindDN(),
				managementConfiguration.getUser().getPassword());
		}
		catch (GeneralSecurityException e)
		{
			log.error("Could not open a management connection to ldap", e);
		}
		return connection;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		userCache = CacheBuilder.newBuilder()
			.expireAfterWrite(60, TimeUnit.MINUTES)
			.build();
	}

	@Scheduled(cron = "${ldap-management.jobs.updateUnmaintained.cronExpr}")
	protected void updateUnmaintainedExternals()
	{
		log.debug("Updating the informations about unmaintained accounts");
		LocalDateTime startTime = LocalDateTime.now();
		LDAPConnection connection = null;
		final int nextWeeks = managementConfiguration.getLeavingUsersInCW();
		List<User> exitedActiveUsers = new LinkedList<>();
		List<User> futureExitingUser = new LinkedList<>();
		try
		{
			connection = createManagementConnection();
			final int userCount = ldapService.getUserCount(connection);
			final int blockSize = 250;
			List<User> users = null;
			LocalDate today = LocalDate.now();
			LocalDate nearFuture = today.plusWeeks(nextWeeks);
			for (int i = 0; i < userCount; i = i + blockSize)
			{
				users = ldapService.getUsers(connection, i, blockSize);
				if (users != null && !users.isEmpty())
				{
					for (User user : users)
					{
						LocalDate exitDate = user.getSzzExitDate();
						if (exitDate != null && user.getSzzStatus() == User.State.active)
						{
							if (exitDate.isBefore(today))
							{
								exitedActiveUsers.add(user);
							}
							else if (exitDate.isBefore(nearFuture))
							{
								futureExitingUser.add(user);
							}
						}
					}
				}
			}
			log.debug("Found {} user who are exited but have active accounts", exitedActiveUsers.size());
			log.debug("Found {} user who are leaving in the next {} weeks", futureExitingUser.size(), nextWeeks);
			userCache.put(EXITED_ACTIVE_USERS, exitedActiveUsers);
			userCache.put(NEAR_FUTURE_EXITING, futureExitingUser);

			log.info("Updated the informations about unmaintained accounts in {}",
				DateTimeHelper.getDurationString(startTime, LocalDateTime.now(), ChronoUnit.MILLIS));
		}
		catch (LDAPException e)
		{
			log.error("Could not update unmaintained users", e);
		}
		finally
		{
			if (connection != null)
			{
				connection.close();
			}
		}
	}

	@Override
	public List<User> getUnmaintainedExternals()
	{
		List<User> res = userCache.getIfPresent(EXITED_ACTIVE_USERS);
		if (res == null)
		{
			res = Collections.emptyList();
		}
		return res;
	}

	@Override
	public List<User> getLeavingUsers()
	{
		List<User> res = userCache.getIfPresent(NEAR_FUTURE_EXITING);
		if (res == null)
		{
			res = Collections.emptyList();
		}
		return res;
	}

	@Override
	public void addDefaultGroups(User user)
	{

	}

	@Override
	public void delDefaulGroups(User user)
	{

	}

}
