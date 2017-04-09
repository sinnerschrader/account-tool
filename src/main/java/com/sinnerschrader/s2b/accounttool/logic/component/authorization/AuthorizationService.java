package com.sinnerschrader.s2b.accounttool.logic.component.authorization;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;

public interface AuthorizationService {

	boolean isAdmin(LdapUserDetails user);

	boolean isUserAdministration(LdapUserDetails user);

	boolean isGroupAdmin(LdapUserDetails user, String groupCn);

	void ensureUserAdministration(LdapUserDetails user) throws UnauthorizedException;

	void ensureGroupAdministration(LdapUserDetails user, String group) throws UnauthorizedException;
}
