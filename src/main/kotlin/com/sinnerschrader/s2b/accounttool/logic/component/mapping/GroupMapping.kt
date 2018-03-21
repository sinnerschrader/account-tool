package com.sinnerschrader.s2b.accounttool.logic.component.mapping

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.logic.entity.Group.GroupClassification.*
import com.sinnerschrader.s2b.accounttool.logic.entity.Group.GroupType
import com.sinnerschrader.s2b.accounttool.logic.entity.GroupOfNames
import com.sinnerschrader.s2b.accounttool.logic.entity.PosixGroup
import com.unboundid.ldap.sdk.SearchResultEntry
import org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Arrays.asList

@Service
class GroupMapping {

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    fun map(entry: SearchResultEntry?): Group? {
        if (entry == null) return null

        fun SearchResultEntry.str(attributeName: String) = getAttributeValue(attributeName)
        fun SearchResultEntry.int(attributeName: String) = getAttributeValueAsInteger(attributeName)

        with(entry) {
            try {
                val objectClasses = asList(*objectClassValues)
                val cn = str("cn")

                return when {
                    isPosixGroup(objectClasses) ->
                        PosixGroup(
                                dn = dn,
                                cn = cn,
                                gid = int("gid"),
                                description = str("description") ?: "",
                                groupClassification = getGroupClassification(cn),
                                memberIds = asList(*getAttributeValues("memberUid") ?: EMPTY_STRING_ARRAY)
                        )
                    isGroupOfUniqueNames(objectClasses) -> GroupOfNames(
                            dn = dn,
                            cn = cn,
                            description = str("description") ?: "",
                            isUniqueNames = true,
                            groupClassification = getGroupClassification(cn),
                            memberIds = asList(*getAttributeValues(GroupType.GroupOfUniqueNames.memberAttritube) ?: EMPTY_STRING_ARRAY)
                    )
                    isGroupOfNames(objectClasses) -> GroupOfNames(
                            dn = dn,
                            cn = cn,
                            description = str("description") ?: "",
                            isUniqueNames = false,
                            groupClassification = getGroupClassification(cn),
                            memberIds = asList(*getAttributeValues(GroupType.GroupOfNames.memberAttritube) ?: EMPTY_STRING_ARRAY)
                    )
                    else ->
                        throw IllegalArgumentException("Provided result entry is not supported. Please call isCompatible before.")
                }
            } catch (e: Exception) {
                LOG.error("failed to map: " + dn, e)
                return null
            }
        }
    }

    private fun isAdminGroup(cn: String) =
            StringUtils.containsAny(cn, "admins", "administrators") ||
                    ldapConfiguration.permissions.ldapAdminGroup == cn ||
                    cn.startsWith(ldapConfiguration.groupPrefixes.admin)

    private fun isTechnicalGroup(cn: String) = cn.startsWith(ldapConfiguration.groupPrefixes.technical)
    private fun isTeamGroup(cn: String) = cn.startsWith(ldapConfiguration.groupPrefixes.team)

    private fun getGroupClassification(cn: String) =
            when {
                isAdminGroup(cn) -> ADMIN
                isTechnicalGroup(cn) -> TECHNICAL
                isTeamGroup(cn) -> TEAM
                else -> UNKNOWN
            }

    private fun isGroupOfNames(objectClasses: Collection<String>) = objectClasses.contains(GroupType.GroupOfNames.objectClass)
    private fun isGroupOfUniqueNames(objectClasses: Collection<String>) = objectClasses.contains(GroupType.GroupOfUniqueNames.objectClass)
    private fun isPosixGroup(objectClasses: Collection<String>) = objectClasses.contains(GroupType.Posix.objectClass)

    fun isCompatible(entry: SearchResultEntry?): Boolean {
        if (entry == null) return false

        val objectClasses = asList(*entry.objectClassValues)
        return isPosixGroup(objectClasses) ||
                isGroupOfNames(objectClasses) ||
                isGroupOfUniqueNames(objectClasses)
    }

    fun map(entries: List<SearchResultEntry>) = entries.filter { isCompatible(it) }.mapNotNull { map(it) }.sorted()

    companion object {
        private val LOG = LoggerFactory.getLogger(GroupMapping::class.java)
    }
}
