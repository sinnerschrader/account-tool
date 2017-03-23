package com.sinnerschrader.s2b.accounttool.logic.component.authorization;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service("authorizationService")
public class AuthorizationServiceImpl implements AuthorizationService {

	@Autowired
	private LdapConfiguration ldapConfiguration;

	private boolean isMemberOf(Collection<? extends GrantedAuthority> authorities, String group) {
		for (GrantedAuthority ga : authorities)
			if (StringUtils.equals(ga.getAuthority(), group)) return true;
		return false;
	}

	@Override
	public boolean isAdmin(LdapUserDetails user) {
		for (String adminGroup : ldapConfiguration.getAdministrationGroups())
			if (isMemberOf(user.getAuthorities(), adminGroup)) return true;
		return false;
	}

	@Override
	public boolean isUserAdministration(LdapUserDetails user) {
		for (String userAdminGroup : ldapConfiguration.getUserAdministrationGroups())
			if (isMemberOf(user.getAuthorities(), userAdminGroup)) return true;
		return false;
	}

	@Override
	public boolean isGroupAdmin(LdapUserDetails user, String groupCn) {
		final String prefixSuffix = "-"; //yepp, a suffix on a prefix.
		final String adminPrefix = ldapConfiguration.getGroupPrefixes().getAdmin() + prefixSuffix;
		final String technicalPrefix =
				ldapConfiguration.getGroupPrefixes().getTechnical() + prefixSuffix;
		final String teamPrefix = ldapConfiguration.getGroupPrefixes().getTeam() + prefixSuffix;
		if (StringUtils.startsWith(groupCn, adminPrefix)
				|| StringUtils.startsWith(groupCn, technicalPrefix)) {
			return isMemberOf(user.getAuthorities(), groupCn);
		}
		if (StringUtils.startsWith(groupCn, teamPrefix)) {
			return isMemberOf(
					user.getAuthorities(), StringUtils.replace(groupCn, teamPrefix, adminPrefix));
		}
		return false;
	}

	@Override
	public void ensureUserAdministration(LdapUserDetails user) throws UnauthorizedException {
		if (isAdmin(user) || isUserAdministration(user)) {
			return;
		}
		throw new UnauthorizedException(
				user.getUsername(), "createOrModifyUser", "User is not member of required groups");
	}

	@Override
	public void ensureGroupAdministration(LdapUserDetails user, String group)
			throws UnauthorizedException {
		if (isAdmin(user) || isGroupAdmin(user, group)) {
			return;
		}
		throw new UnauthorizedException(
				user.getUsername(), "addOrRemoveUserFromGroup", "User is not member of required group");
	}
}
