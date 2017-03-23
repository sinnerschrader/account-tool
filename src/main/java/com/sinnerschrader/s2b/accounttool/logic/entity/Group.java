package com.sinnerschrader.s2b.accounttool.logic.entity;

import org.apache.commons.lang3.StringUtils;

import java.beans.Transient;
import java.util.List;

/**
 * Interface for different Implementations of Groups. We currently use mainly posixGroups in LDAP,
 * but will use also groupOfNames or groupOfUniqueNames. This Interface combines the relevant
 * informations of all of them and provides a type information
 */
public interface Group extends Comparable<Group> {

	/**
	 * The LDAP DN of the Group
	 *
	 * @return dn
	 */
	String getDn();

	/**
	 * The name of the group
	 *
	 * @return cn
	 */
	String getCn();

	/**
	 * A short description about this Group
	 *
	 * @return description
	 */
	String getDescription();

	/**
	 * Contains the list of UIDs or DNs of the Users which are member of the current group.
	 *
	 * @return List of UIDs or DNs
	 */
	List<String> getMemberIds();

	/**
	 * Indentification of LDAP Group Type (Posix, GroupOfNames, etc).
	 *
	 * @return the type, which ldap group type is used.
	 */
	GroupType getGroupType();

	/**
	 * Returns a array of all required ObjectClasses for this Group Instance
	 *
	 * @return array of ldap objectClass attribute values
	 */
	List<String> getObjectClasses();

	/**
	 * Checks if the current uidOrDN is a member of this group. You have to use this method twice, to
	 * check if a user is member of the group. Prefer hasMember(User user) where posible.
	 *
	 * @param uidOrDN the uid or dn of the user.
	 * @return if the uid or dn is in the list.
	 */
	default boolean hasMember(String uidOrDN) {
		return getMemberIds().contains(uidOrDN);
	}

	default boolean hasMember(User user) {
		return hasMember(user.getUid()) || hasMember(user.getDn());
	}

	@Transient
	GroupClassification getGroupClassification();

	/**
	 * Checks if this group is an admin group for a project or internal team.
	 *
	 * @return state if it is an admin group or not.
	 */
	@Transient
	default boolean isAdminGroup() {
		return getGroupClassification() == GroupClassification.ADMIN;
	}

	/**
	 * Checks if this group is an admin group for a project or internal team.
	 *
	 * @return state if it is an admin group or not.
	 */
	@Transient
	default boolean isTechnicalGroup() {
		return getGroupClassification() == GroupClassification.TECHNICAL;
	}

	/**
	 * Checks if this group is an admin group for a project or internal team.
	 *
	 * @return state if it is an admin group or not.
	 */
	@Transient
	default boolean isTeamGroup() {
		return getGroupClassification() == GroupClassification.TEAM;
	}

	/**
	 * Extracts the Prefix from group cn. This prefixes handles some classification on groups.
	 * <p>
	 * <p>Example: devs-customer, admin-customer or team-customer will result in "devs", "admin" and
	 * "team"
	 *
	 * @return the prefix of the group or empty string if the group name is not prefixed.
	 */
	@Transient
	default String getGroupPrefix() {
		final String cn = getCn();
		final String separator = "-";
		if (cn.contains(separator)) {
			return cn.split(separator)[0];
		}
		return "";
	}

	/**
	 * Returns the Name of the Group without the prefix of the group. If there are several groups for
	 * the same area/customer this will return the same name.
	 * <p>
	 * <p>Example: team-customer, admin-customer or devs-customer will result on both to "customer"
	 *
	 * @return the name without prefix
	 */
	@Transient
	default String getName() {
		final String separator = "-";
		final String cn = getCn();
		int pos = Math.max(cn.indexOf(separator) + 1, 0);
		return (pos >= cn.length()) ? cn : cn.substring(pos);
	}

	@Override
	default int compareTo(Group o) {
		int res = StringUtils.compareIgnoreCase(getName(), o.getName());
		if (res == 0) {
			res = StringUtils.compareIgnoreCase(getGroupPrefix(), o.getGroupPrefix());
		}
		return res;
	}

	/**
	 * Classification of a Group if it is a Client Team Admin Group, Client Team Group, Technical
	 * Integration Group or unknown.
	 */
	enum GroupClassification {
		ADMIN,
		TEAM,
		TECHNICAL,
		UNKNOWN;
	}

	/**
	 * The LDAP Type which the Group Instance is respresenting.
	 */
	enum GroupType {

		/**
		 * Spec: https://tools.ietf.org/html/rfc2307 Chapter: 2.2 and 4.
		 */
		Posix("posixGroup", "memberUid"),

		/**
		 * Spec: https://tools.ietf.org/html/rfc4519#page-22 Chapter: 3.5
		 */
		GroupOfNames("groupOfNames", "member"),

		/**
		 * Spec: https://tools.ietf.org/html/rfc4519#page-22 Chapter: 3.6
		 */
		GroupOfUniqueNames("groupOfUniqueNames", "uniqueMember");

		private final String objectClass;

		private final String memberAttritube;

		GroupType(String objectClass, String memberAttritube) {
			this.objectClass = objectClass;
			this.memberAttritube = memberAttritube;
		}

		public String getObjectClass() {
			return objectClass;
		}

		public String getMemberAttritube() {
			return memberAttritube;
		}
	}
}
