package com.sinnerschrader.s2b.accounttool.logic.component.licences;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by vikgru on 24/01/2017.
 */
public enum DependencyType {
	UNKNOWN,
	MAVEN,
	NPM;

	public static DependencyType parse(final String name, final DependencyType defaultType) {
		final String sanitizedName = StringUtils.trimToEmpty(name);
		for (DependencyType dt : values()) {
			if (StringUtils.equalsAnyIgnoreCase(sanitizedName, dt.name())) {
				return dt;
			}
		}
		return defaultType;
	}
}
