package com.sinnerschrader.s2b.accounttool.config.authentication;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;


public class LdapAuthenticationDetailsSource extends WebAuthenticationDetailsSource
{

	private LdapConfiguration ldapConfiguration;

	public LdapAuthenticationDetailsSource(LdapConfiguration ldapConfiguration)
	{
		this.ldapConfiguration = ldapConfiguration;
	}

	@Override
	public WebAuthenticationDetails buildDetails(HttpServletRequest context)
	{
		return new LdapAuthenticationDetails(ldapConfiguration, context);
	}

}
