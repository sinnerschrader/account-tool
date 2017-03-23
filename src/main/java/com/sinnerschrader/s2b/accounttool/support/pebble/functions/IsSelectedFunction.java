package com.sinnerschrader.s2b.accounttool.support.pebble.functions;

import com.mitchellbosecke.pebble.extension.Function;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Helper Function for check option on select fields. This functions return the selected attribute
 */
public class IsSelectedFunction implements Function {

	public static final String FUNCTION_NAME = "isSelected";

	private static final String VALUE_PARAM_NAME = "current";

	private static final String MATCH_PARAM_NAME = "match";

	public IsSelectedFunction() {
	}

	@Override
	public List<String> getArgumentNames() {
		return Arrays.asList(VALUE_PARAM_NAME, MATCH_PARAM_NAME);
	}

	@Override
	public Object execute(Map<String, Object> args) {
		LdapUserDetails currentUser = RequestUtils.getCurrentUserDetails();
		String value = (String) args.get(VALUE_PARAM_NAME);
		String match = (String) args.get(MATCH_PARAM_NAME);
		return StringUtils.equals(value, match) ? "selected" : "";
	}
}
