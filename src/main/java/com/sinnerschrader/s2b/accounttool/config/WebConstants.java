package com.sinnerschrader.s2b.accounttool.config;

/**
 * Class for collecting all application wide required constants.
 */
public final class WebConstants {

	public static final String COMPANY_COOKIE_NAME = "s2act_company";

	public static final int COMPANY_COOKIE_MAXAGE = 90 * 24 * 60 * 60;

	public static final String ATTR_CONNECTION = "s2act_ldap-connection";

	private WebConstants() {
	}
}
