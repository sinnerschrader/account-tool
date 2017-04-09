package com.sinnerschrader.s2b.accounttool.logic.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * GroupOfNames represents the
 */
public class GroupOfNames implements Group {

	private final List<String> objectClasses;

	private final String dn;

	private final String cn;

	private final String description;

	private final boolean uniqueNames;

	private final List<String> memberDNs;

	private transient GroupClassification groupClassification;

	public GroupOfNames(
			String dn,
			String cn,
			String description,
			boolean uniqueNames,
			GroupClassification groupClassification,
			String... memberDNs) {
		final GroupType groupType = uniqueNames ? GroupType.GroupOfUniqueNames : GroupType.GroupOfNames;

		this.objectClasses =
				Collections.unmodifiableList(Arrays.asList("top", groupType.getObjectClass()));
		this.dn = dn;
		this.cn = cn;
		this.description = description;
		this.uniqueNames = uniqueNames;
		this.groupClassification = groupClassification;
		this.memberDNs =
				(memberDNs != null)
						? Collections.unmodifiableList(Arrays.asList(memberDNs))
						: Collections.emptyList();
	}

	@Override
	public GroupClassification getGroupClassification() {
		return groupClassification;
	}

	@Override
	public List<String> getObjectClasses() {
		return objectClasses;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<String> getMemberIds() {
		return memberDNs;
	}

	@Override
	public GroupType getGroupType() {
		return uniqueNames ? GroupType.GroupOfUniqueNames : GroupType.GroupOfNames;
	}

	@Override
	public String getDn() {
		return dn;
	}

	@Override
	public String getCn() {
		return cn;
	}

	public boolean isUniqueNames() {
		return uniqueNames;
	}
}
