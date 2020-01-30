package com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2

import com.sinnerschrader.s2b.accounttool.config.UserConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.UserMapping
import com.sinnerschrader.s2b.accounttool.logic.entity.GroupInfo
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import com.unboundid.ldap.sdk.Filter.*
import com.unboundid.ldap.sdk.SearchRequest.ALL_OPERATIONAL_ATTRIBUTES
import com.unboundid.ldap.sdk.SearchRequest.ALL_USER_ATTRIBUTES
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.servlet.http.HttpServletRequest

@Service
class LdapServiceV2 {
    @Autowired
    var request: HttpServletRequest? = null

    @Autowired
    lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    lateinit var userConfiguration: UserConfiguration

    @Autowired
    lateinit var userMapping: UserMapping

    fun getGroups(cn: String? = null, memberUid: String? = null) =
            // TODO fail on no request at the moment
            with(RequestUtils.getLdapConnection(request!!)!!) {
                search(ldapConfiguration.config.groupDN, SearchScope.SUB,
                        createANDFilter(
                                listOfNotNull(
                                        createORFilter(
                                                createEqualityFilter("objectClass", "posixGroup"),
                                                createEqualityFilter("objectClass", "groupOfUniqueNames"),
                                                createEqualityFilter("objectClass", "groupOfNames")
                                        ),
                                        cn?.let { createEqualityFilter("cn", cn) },
                                        memberUid?.let { createEqualityFilter("memberuid", memberUid) }
                                )
                        ),
                        "cn", "description", "memberuid"
                )
            }.searchEntries.map {
                GroupInfo(
                        name = it.getAttributeValue("cn"),
                        description = it.getAttributeValue("description") ?: "",
                        members = (it.getAttributeValues("memberuid") ?: emptyArray()).toSortedSet())
            }

    enum class UserAttributes(vararg val ldapAttributes: String){
        FULL(ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES),
        INFO("uid", "givenName", "sn", "mail", "szzStatus", "description", "szzExternalAccounts")
    }

    fun getUser(uid: String? = null,
                state: State? = null,
                company: String? = null,
                type: String? = null,
                searchTerm: String? = null,
                entryDateRange: DateRange? = null,
                exitDateRange: DateRange? = null,
                attributes: UserAttributes = UserAttributes.INFO) =
            // TODO fail on no request at the moment
            with(RequestUtils.getLdapConnection(request!!)!!) {
                search(
                        (company?.let { "ou=$it," } ?: "") + ldapConfiguration.config.baseDN,
                        SearchScope.SUB,
                        createANDFilter(
                                listOfNotNull(
                                        createEqualityFilter("objectclass", "posixAccount"),
                                        uid?.let { createEqualityFilter("uid", uid) },
                                        state?.let { createEqualityFilter("szzStatus", state.name) },
                                        type?.let { createEqualityFilter("description", type) },
                                        searchTerm?.let { createUserSearchFilter(searchTerm) },
                                        entryDateRange?.createFilter("szzEntryDate"),
                                        exitDateRange?.createFilter("szzExitDate")
                                )
                        ),
                        *attributes.ldapAttributes
                ).searchEntries.mapNotNull { userMapping.map(it)}
            }

    fun SearchResultEntry.int(attributeName: String) = getAttributeValueAsInteger(attributeName) ?: 0
    fun SearchResultEntry.long(attributeName: String) = getAttributeValueAsLong(attributeName) ?: 0L
    fun SearchResultEntry.str(attributeName: String) = getAttributeValue(attributeName) ?: ""
    fun SearchResultEntry.strMap(attributeName: String) =
            if (hasAttribute(attributeName)) str(attributeName).split(',')
                    .mapNotNull {
                        it.split('=', limit = 2).let { keyValue ->
                            if (keyValue.size == 2) keyValue[0] to keyValue[1] else null
                        }
                    }.toMap()
            else emptyMap()

    fun companyForDn(dn: String) =
            try {
                with(Regex(",ou=([^,]+)").findAll(dn).last().groupValues[1]) {
                    ldapConfiguration.companies[this] ?: "UNKNOWN"
                }
            } catch (e: NoSuchElementException) {
                "UNKNOWN"
            }

    private fun createUserSearchFilter(search: String) = createORFilter(
            listOf("uid", "givenName", "sn", "mail", "cn").map {
                createSubstringFilter(it, null, arrayOf(search), null)
            })

    data class DateRange(val from: LocalDate = DateRange.MIN, val to: LocalDate = DateRange.MAX) {
        companion object {
            val MIN = LocalDate.of(0,1,1)!!
            val MAX = LocalDate.of(9999,12,12)!!

            fun of(from: LocalDate?, to: LocalDate?) = when {
                from == null && to == null -> null
                else -> DateRange(from ?: DateRange.MIN, to ?: DateRange.MAX)
            }
        }

        fun createFilter(attributeName: String) = createANDFilter(
                // TODO handle unset dates
                createGreaterOrEqualFilter(attributeName, from.format(DateTimeFormatter.ISO_DATE)),
                createLessOrEqualFilter(attributeName, to.format(DateTimeFormatter.ISO_DATE))
        )
    }
}
