package com.sinnerschrader.s2b.accounttool.support.pebble;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.support.pebble.functions.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Extension for Pebble support which are required for the Tool
 */
public class AccountToolExtension extends AbstractExtension {

	@Autowired
	private AuthorizationService authorizationService;

	public Map<String, Function> getFunctions() {
		if (authorizationService == null) {
			throw new IllegalStateException("The AuthorizationService is not allowed to be null");
		}

		Map<String, Function> functions = new HashMap<>();

		// Authozization functions
		functions.put(IsLoggedInFunction.FUNCTION_NAME, new IsLoggedInFunction());
		functions.put(
				IsGroupAdminFunction.FUNCTION_NAME, new IsGroupAdminFunction(authorizationService));
		functions.put(IsAdminFunction.FUNCTION_NAME, new IsAdminFunction(authorizationService));
		functions.put(IsUserAdminFunction.FUNCTION_NAME, new IsUserAdminFunction(authorizationService));
		functions.put(IsMemberOfFunction.FUNCTION_NAME, new IsMemberOfFunction(authorizationService));

		// Helper functions
		functions.put(IsSelectedFunction.FUNCTION_NAME, new IsSelectedFunction());

		return functions;
	}
}
