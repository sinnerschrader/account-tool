package com.sinnerschrader.s2b.accounttool.support.pebble.functions;

import com.mitchellbosecke.pebble.extension.Function;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helper Function to check if a user is member of a group
 */
public class IsMemberOfFunction implements Function {

	public static final String FUNCTION_NAME = "isMemberOf";

	private static final String GROUP_PARAM_NAME = "groupCn";

	private AuthorizationService authorizationService;

	public IsMemberOfFunction(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	@Override
	public List<String> getArgumentNames() {
		return Collections.singletonList(GROUP_PARAM_NAME);
	}

	@Override
	public Object execute(Map<String, Object> args) {
		LdapUserDetails userDetails = RequestUtils.getCurrentUserDetails();
		Object groupArgument = args.get(GROUP_PARAM_NAME);
		Group group = null;
		String groupCn;
		if (groupArgument instanceof Group) {
			group = (Group) args.get(GROUP_PARAM_NAME);
			groupCn = group.getCn();
		} else {
			groupCn = (String) args.get(GROUP_PARAM_NAME);
		}

		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
		if (authorities != null) {
			for (GrantedAuthority ga : authorities) {
				if (StringUtils.equals(ga.getAuthority(), groupCn)) {
					return true;
				}
			}
		}
		return group != null
				&& (group.hasMember(userDetails.getUid()) || group.hasMember(userDetails.getDn()));
	}
}
