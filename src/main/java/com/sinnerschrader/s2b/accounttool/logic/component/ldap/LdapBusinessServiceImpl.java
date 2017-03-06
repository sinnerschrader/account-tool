package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 *
 */
@Component
public class LdapBusinessServiceImpl implements LdapBusinessService, InitializingBean
{

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private LdapManagementConfiguration managementConfiguration;

	protected LDAPConnection createManagementConnection() throws LDAPException
	{
		LDAPConnection connection = ldapConfiguration.createConnection();
		connection.bind(managementConfiguration.getUser().getBindDN(), managementConfiguration.getUser().getPassword());
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
