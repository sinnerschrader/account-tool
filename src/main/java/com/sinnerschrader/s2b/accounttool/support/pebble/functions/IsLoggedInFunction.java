package com.sinnerschrader.s2b.accounttool.support.pebble.functions;

import com.mitchellbosecke.pebble.extension.Function;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Simple Function to check if a user is logged in or not.
 */
public class IsLoggedInFunction implements Function {

	public static final String FUNCTION_NAME = "isLoggedIn";

	@Override
	public List<String> getArgumentNames() {
		return Collections.emptyList();
	}

	@Override
	public Object execute(Map<String, Object> args) {
		return RequestUtils.getCurrentUserDetails() != null;
	}
}
