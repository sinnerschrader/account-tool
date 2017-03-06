package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 *
 */
@Component
public class LdapBusinessServiceImpl implements LdapBusinessService, InitializingBean
{

	private final static Logger log = LoggerFactory.getLogger(LdapBusinessServiceImpl.class);

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

	}

	@Override
	public void getUnmaintainedExternals()
	{

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
