package com.sinnerschrader.s2b.accounttool.logic.entity

import java.util.Arrays.asList

data class GroupOfNames(
    override val dn: String,
    override val cn: String,
    override val description: String,
    val isUniqueNames: Boolean,
    override val groupClassification: Group.GroupClassification,
    override val memberIds: List<String>) : Group {

    override val objectClasses =
        asList("top", if (isUniqueNames) Group.GroupType.GroupOfUniqueNames.objectClass else Group.GroupType.GroupOfNames.objectClass)

    override val groupType: Group.GroupType =
        if (isUniqueNames) Group.GroupType.GroupOfUniqueNames else Group.GroupType.GroupOfNames
}
