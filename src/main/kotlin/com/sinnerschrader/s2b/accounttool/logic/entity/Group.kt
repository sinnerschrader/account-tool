package com.sinnerschrader.s2b.accounttool.logic.entity

import org.apache.commons.lang3.builder.CompareToBuilder


/**
 * Interface for different Implementations of Groups.
 * We currently use mainly posixGroups in LDAP, but will use also groupOfNames or groupOfUniqueNames.
 * This Interface combines the relevant informations of all of them and provides a type information
 *
 * @param dn LDAP DN of the Group
 * @param cn The name of the group
 * @param description A short description about this Group
 * @param memberIds Contains the list of UIDs or DNs of the Users which are member of the current group.
 * @param groupType Indentification of LDAP Group Type (Posix, GroupOfNames, etc).
 * @param objectClasses  Returns a array of all required ObjectClasses for this Group Instance
 */
interface Group : Comparable<Group> {
    val dn: String
    val cn: String
    val description: String
    val memberIds: List<String>
    val groupType: GroupType
    val objectClasses: List<String>
    val groupClassification: GroupClassification

    val groupPrefix: String get() = cn.substringBefore("-", missingDelimiterValue = "")
    val name: String get() = cn.substringAfter("-")

    fun hasMember(uid: String, dn: String) = memberIds.contains(uid) || memberIds.contains(dn)
    override fun compareTo(other: Group) = CompareToBuilder().append(name, other.name).append(groupPrefix, other.groupPrefix).build()!!

    /**
     * Classification of a Group if it is a Client Team Admin Group, Client Team Group, Technical Integration Group or unknown.
     */
    enum class GroupClassification {ADMIN, TEAM, TECHNICAL, UNKNOWN }

    /**
     * The LDAP Type which the Group Instance is representing.
     *
     * @property Posix Spec: https://tools.ietf.org/html/rfc2307 [Chapter: 2.2 and 4.]
     * @property GroupOfNames Spec: https://tools.ietf.org/html/rfc4519#page-22 [Chapter: 3.5]
     * @property GroupOfUniqueNames Spec: https://tools.ietf.org/html/rfc4519#page-22 [Chapter: 3.6]
     */
    enum class GroupType(val objectClass: String, val memberAttritube: String) {
        Posix("posixGroup", "memberUid"),
        GroupOfNames("groupOfNames", "member"),
        GroupOfUniqueNames("groupOfUniqueNames", "uniqueMember")
    }
}
