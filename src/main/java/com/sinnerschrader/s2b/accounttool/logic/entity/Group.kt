package com.sinnerschrader.s2b.accounttool.logic.entity

import org.apache.commons.lang3.builder.CompareToBuilder
import org.springframework.util.CollectionUtils.containsAny
import java.util.Arrays.asList


/**
 * Interface for different Implementations of Groups.
 * We currently use mainly posixGroups in LDAP, but will use also groupOfNames or groupOfUniqueNames.
 * This Interface combines the relevant informations of all of them and provides a type information
 */
interface Group : Comparable<Group> {

    /** The LDAP DN of the Group */
    val dn: String

    /** The name of the group */
    val cn: String

    /** A short description about this Group */
    val description: String

    /** Contains the list of UIDs or DNs of the Users which are member of the current group. */
    val memberIds: List<String>

    /** Indentification of LDAP Group Type (Posix, GroupOfNames, etc). */
    val groupType: GroupType

    /** Returns a array of all required ObjectClasses for this Group Instance */
    val objectClasses: List<String>

    fun hasMember(uid: String, dn: String) = containsAny(memberIds, asList(uid, dn))

    val groupClassification: GroupClassification

    /**
     * Checks if this group is an admin group for a project or internal team.
     */
    val isAdminGroup: Boolean get() = groupClassification == GroupClassification.ADMIN

    /**
     * Checks if this group is an admin group for a project or internal team.
     */
    val isTechnicalGroup: Boolean get() = groupClassification == GroupClassification.TECHNICAL

    /**
     * Checks if this group is an admin group for a project or internal team.
     */
    val isTeamGroup: Boolean get() = groupClassification == GroupClassification.TEAM

    /**
     * Extracts the Prefix from group cn. This prefixes handles some classification on groups.
     */
    val groupPrefix: String get() = cn.substringBefore("-", missingDelimiterValue = "")

    /**
     * Returns the Name of the Group without the prefix of the group. If there are several groups for the same
     * area/customer this will return the same name.
     */
    val name: String get() = cn.substringAfter("-")

    override fun compareTo(other: Group): Int =
        CompareToBuilder().append(name, other.name).append(groupPrefix, other.groupPrefix).build()

    /**
     * Classification of a Group if it is a Client Team Admin Group, Client Team Group, Technical Integration Group or unknown.
     */
    enum class GroupClassification {ADMIN, TEAM, TECHNICAL, UNKNOWN }

    /**
     * The LDAP Type which the Group Instance is representing.
     */
    enum class GroupType private constructor(val objectClass: String, val memberAttritube: String) {

        /**
         * Spec: https://tools.ietf.org/html/rfc2307
         * Chapter: 2.2 and 4.
         */
        Posix("posixGroup", "memberUid"),

        /**
         * Spec: https://tools.ietf.org/html/rfc4519#page-22
         * Chapter: 3.5
         */
        GroupOfNames("groupOfNames", "member"),

        /**
         * Spec: https://tools.ietf.org/html/rfc4519#page-22
         * Chapter: 3.6
         */
        GroupOfUniqueNames("groupOfUniqueNames", "uniqueMember")

    }

}
