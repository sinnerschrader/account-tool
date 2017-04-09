package com.sinnerschrader.s2b.accounttool.logic.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Groups for Customers and Internal Permission on LDAP
 */
public class PosixGroup implements Group {

	private static final List<String> objectClasses =
			Collections.unmodifiableList(Arrays.asList("top", GroupType.Posix.getObjectClass()));

	private final String dn;

	private final String cn;

	private final Integer gid;

	private final String description;

	private final List<String> memberUids;

	private transient GroupClassification groupClassification;

	public PosixGroup(
			String dn,
			String cn,
			Integer gid,
			String description,
			GroupClassification groupClassification,
			String... memberUids) {
		this.dn = dn;
		this.cn = cn;
		this.gid = gid;
		this.description = description;
		this.groupClassification = groupClassification;
		this.memberUids =
				(memberUids != null)
						? Collections.unmodifiableList(Arrays.asList(memberUids))
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
	public GroupType getGroupType() {
		return GroupType.Posix;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PosixGroup)) return false;

		PosixGroup group = (PosixGroup) o;

		if (dn != null ? !dn.equals(group.dn) : group.dn != null) return false;
		if (cn != null ? !cn.equals(group.cn) : group.cn != null) return false;
		return gid != null ? gid.equals(group.gid) : group.gid == null;
	}

	@Override
	public int hashCode() {
		int result = dn != null ? dn.hashCode() : 0;
		result = 31 * result + (cn != null ? cn.hashCode() : 0);
		result = 31 * result + (gid != null ? gid.hashCode() : 0);
		return result;
	}

	@Override
	public String getDn() {
		return dn;
	}

	@Override
	public String getCn() {
		return cn;
	}

	public Integer getGid() {
		return gid;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<String> getMemberIds() {
		return memberUids;
	}
}
