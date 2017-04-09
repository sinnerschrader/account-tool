package com.sinnerschrader.s2b.accounttool.presentation;

import com.sinnerschrader.s2b.accounttool.config.WebConstants;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.unboundid.ldap.sdk.LDAPConnection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;

/** */
public class RequestUtils {

	private RequestUtils() {
	}

	public static LDAPConnection getLdapConnection(HttpServletRequest request) {
		return (LDAPConnection) request.getAttribute(WebConstants.ATTR_CONNECTION);
	}

	public static void setLdapConnection(HttpServletRequest request, LDAPConnection connection) {
		request.setAttribute(WebConstants.ATTR_CONNECTION, connection);
	}

	public static LdapUserDetails getCurrentUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
			return (LdapUserDetails) auth.getPrincipal();
		}
		return null;
	}
}
