package com.sinnerschrader.s2b.accounttool.config.ldap;

/**
 * Configuration Class for handling group prefixes.
 */
public class LdapGroupPrefixes {

	private String admin;

	private String team;

	private String technical;

	public String getAdmin() {
		return admin;
	}

	public void setAdmin(String admin) {
		this.admin = admin;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getTechnical() {
		return technical;
	}

	public void setTechnical(String technical) {
		this.technical = technical;
	}
}
