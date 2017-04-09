package com.sinnerschrader.s2b.accounttool.presentation.interceptor;

import com.sinnerschrader.s2b.accounttool.config.WebConstants;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LdapConnectionInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(LdapConnectionInterceptor.class);

	private LdapConfiguration ldapConfiguration;

	public LdapConnectionInterceptor(LdapConfiguration ldapConfiguration) {
		this.ldapConfiguration = ldapConfiguration;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		if (details != null) {
			LDAPConnection connection = null;
			try {
				connection = ldapConfiguration.createConnection();
				BindResult bindResult = connection.bind(details.getDn(), details.getPassword());
				if (bindResult.getResultCode() == ResultCode.SUCCESS) {
					RequestUtils.setLdapConnection(request, connection);
				}
			} catch (LDAPException le) {
				if (connection != null) {
					connection.close();
				}
				request.getSession().invalidate();
				return false;
			}
		}
		return true;
	}

	@Override
	public void postHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler,
			ModelAndView modelAndView)
			throws Exception {
		LDAPConnection connection = RequestUtils.getLdapConnection(request);
		if (connection != null) {
			connection.close();
		}
		request.removeAttribute(WebConstants.ATTR_CONNECTION);
		LdapUserDetails currentUser = RequestUtils.getCurrentUserDetails();
		if (currentUser != null && modelAndView != null) {
			modelAndView.addObject("currentUser", currentUser);
		}
	}

	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}
}
