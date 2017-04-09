package com.sinnerschrader.s2b.accounttool.config.authentication;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class LdapAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

	private LdapConfiguration ldapConfiguration;

	public LdapAuthenticationDetailsSource(LdapConfiguration ldapConfiguration) {
		this.ldapConfiguration = ldapConfiguration;
	}

	@Override
	public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
		return new LdapAuthenticationDetails(ldapConfiguration, context);
	}
}
