package com.sinnerschrader.s2b.accounttool.logic.entity

import java.util.Arrays.asList

/**
 * Groups for Customers and Internal Permission on LDAP
 */
data class PosixGroup(override val dn: String, override val cn: String, val gid: Int?, override val description: String,
                      override val groupClassification: Group.GroupClassification, override val memberIds: List<String>) : Group {

    override val groupType = Group.GroupType.Posix
    override val objectClasses: List<String> = asList("top", Group.GroupType.Posix.objectClass)
}