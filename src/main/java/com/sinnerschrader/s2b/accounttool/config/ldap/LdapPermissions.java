package com.sinnerschrader.s2b.accounttool.config.ldap;

import org.springframework.beans.factory.InitializingBean;

import java.util.List;

public class LdapPermissions implements InitializingBean {

	private List<String> admins = null;

	private List<String> userAdmins = null;

	private List<String> defaultGroups = null;

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	public List<String> getAdmins() {
		return admins;
	}

	public List<String> getUserAdmins() {
		return userAdmins;
	}

	public List<String> getDefaultGroups() {
		return defaultGroups;
	}

	public void setAdmins(List<String> admins) {
		this.admins = admins;
	}

	public void setUserAdmins(List<String> userAdmins) {
		this.userAdmins = userAdmins;
	}

	public void setDefaultGroups(List<String> defaultGroups) {
		this.defaultGroups = defaultGroups;
	}
}
