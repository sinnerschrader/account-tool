package com.sinnerschrader.s2b.accounttool.support.pebble.functions;

import com.mitchellbosecke.pebble.extension.Function;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Pebble Function to check if the current user is admin
 */
public class IsUserAdminFunction implements Function {

	public static final String FUNCTION_NAME = "isUserAdmin";

	private AuthorizationService authorizationService;

	public IsUserAdminFunction(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	@Override
	public List<String> getArgumentNames() {
		return Collections.emptyList();
	}

	@Override
	public Object execute(Map<String, Object> args) {
		LdapUserDetails currentUser = RequestUtils.getCurrentUserDetails();
		return authorizationService.isUserAdministration(currentUser)
				|| authorizationService.isAdmin(currentUser);
	}
}
