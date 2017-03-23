package com.sinnerschrader.s2b.accounttool.config.authentication;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.LogService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class LdapUserDetailsAuthenticationProvider
		extends AbstractUserDetailsAuthenticationProvider {

	private static final Logger log =
			LoggerFactory.getLogger(LdapUserDetailsAuthenticationProvider.class);

	@Autowired
	private LogService logService;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private LdapService ldapService;

	@Override
	protected UserDetails retrieveUser(
			String username, UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		LDAPConnection connection = null;
		final LdapAuthenticationDetails details =
				(LdapAuthenticationDetails) authentication.getDetails();
		try {
			connection = ldapConfiguration.createConnection();
			BindResult bindResult = connection.bind(details.getUserDN(), details.getPassword());
			if (bindResult.getResultCode() == ResultCode.SUCCESS) {
				return createUserDetails(connection, details, retrieveGroups(connection, details));
			}
			throw new LDAPException(bindResult.getResultCode());
		} catch (AuthenticationException ae) {
			logService.event("logging.logstash.event.login", "failure", details.getUsername());
			throw ae;
		} catch (LDAPException le) {
			final String msg = le.getMessage();
			log.warn(
					msg + "; [uid: {}, code: {}, msg: {}]",
					details.getUsername(),
					le.getResultCode().intValue(),
					le.getResultCode().getName());
			logService.event("logging.logstash.event.login", "failure", details.getUsername());
			throw new BadCredentialsException(le.getResultCode().getName(), le);
		} catch (Exception e) {
			final String msg = "Authentication gone totally wrong";
			log.error(msg, e);
			logService.event("logging.logstash.event.login", "failure", details.getUsername());
			throw new AuthenticationServiceException(msg, e);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private LdapUserDetails createUserDetails(
			LDAPConnection connection, LdapAuthenticationDetails details, List<GrantedAuthority> groups)
			throws AuthenticationException {
		User currentUser = ldapService.getUserByUid(connection, details.getUsername());
		if (currentUser.getSzzStatus() != User.State.active) {
			throw new DisabledException("Inactive accounts can't login");
		}
		log.info("User {} logged in successful", currentUser.getUid());
		logService.event("logging.logstash.event.login", "success", currentUser.getUid());
		return new LdapUserDetails(
				details.getUserDN(),
				details.getUsername(),
				currentUser.getDisplayName(),
				details.getPassword(),
				details.getCompany(),
				groups,
				currentUser.getSzzStatus() != User.State.active,
				currentUser.getSzzStatus() == User.State.active);
	}

	private List<GrantedAuthority> retrieveGroups(
			LDAPConnection connection, LdapAuthenticationDetails details) throws AuthenticationException {
		List<GrantedAuthority> groups = new LinkedList<>();
		ldapService
				.getGroupsByUser(connection, details.getUsername(), details.getUserDN())
				.forEach(group -> groups.add(new SimpleGrantedAuthority(group.getCn())));
		return groups;
	}

	@Override
	protected void additionalAuthenticationChecks(
			UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		// nothing to do here
	}
}
