package com.sinnerschrader.s2b.accounttool.config.authentication;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

public class LdapAuthenticationDetails extends WebAuthenticationDetails {

	private final String userDN;

	private final String username;

	private final String password;

	private final String company;

	LdapAuthenticationDetails(LdapConfiguration ldapConfiguration, HttpServletRequest request) {
		super(request);
		this.username = request.getParameter("uid");
		this.password = request.getParameter("password");
		this.company = request.getParameter("company");
		this.userDN = ldapConfiguration.getUserBind(username, company);
	}

	public String getUserDN() {
		return userDN;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getCompany() {
		return company;
	}
}
