package com.sinnerschrader.s2b.accounttool.support.pebble.functions;

import com.mitchellbosecke.pebble.extension.Function;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Pebble Function to check on markup if the current user is a groupAdmin
 */
public class IsGroupAdminFunction implements Function {

	public static final String FUNCTION_NAME = "isGroupAdmin";

	private static final String GROUP_PARAM_NAME = "groupCn";

	private AuthorizationService authorizationService;

	public IsGroupAdminFunction(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	@Override
	public List<String> getArgumentNames() {
		return Collections.singletonList(GROUP_PARAM_NAME);
	}

	@Override
	public Object execute(Map<String, Object> args) {
		LdapUserDetails currentUser = RequestUtils.getCurrentUserDetails();
		String groupCn = (String) args.get(GROUP_PARAM_NAME);
		return authorizationService.isGroupAdmin(currentUser, groupCn)
				|| authorizationService.isAdmin(currentUser);
	}
}
