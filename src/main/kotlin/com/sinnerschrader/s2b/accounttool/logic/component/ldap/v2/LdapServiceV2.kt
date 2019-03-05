package com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.GroupInfo
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State
import com.sinnerschrader.s2b.accounttool.logic.entity.UserInfo
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import com.unboundid.ldap.sdk.Filter.*
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

    fun getGroups(cn: String? = null, memberUid: String? = null) =
            try {
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
                            description = it.getAttributeValue("description"),
                            members = (it.getAttributeValues("memberuid") ?: emptyArray()).toSortedSet())
                }
            } catch (e: Exception) {
                throw RuntimeException("BOOOM", e)
            }

    fun getUser(uid: String? = null,
                state: State? = null,
                search: String? = null,
                entryDateRange: DateRange? = null,
                exitDateRange: DateRange? = null) =
            try {
                with(RequestUtils.getLdapConnection(request!!)!!) {
                    search(
                            ldapConfiguration.config.baseDN,
                            SearchScope.SUB,
                            createANDFilter(
                                    listOfNotNull(
                                            createEqualityFilter("objectclass", "posixAccount"),
                                            uid?.let { createEqualityFilter("uid", uid) },
                                            state?.let { createEqualityFilter("szzStatus", state.name) },
                                            search?.let { createUserSearchFilter(search) },
                                            entryDateRange?.createFilter("szzEntryDate"),
                                            exitDateRange?.createFilter("szzExitDate")
                                    )
                            ),
                            "uid", "givenName", "sn", "mail", "szzStatus"
                    ).searchEntries.map {
                        UserInfo(
                                dn = it.dn,
                                uid = it.getAttributeValue("uid"),
                                givenName = it.getAttributeValue("givenName"),
                                sn = it.getAttributeValue("sn"),
                                o = companyForDn(it.dn),
                                mail = it.getAttributeValue("mail"),
                                szzStatus = State.valueOf(it.getAttributeValue("szzStatus")))

                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("BOOOOM")
            }

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

    data class DateRange(val from: LocalDate = LocalDate.MIN, val to: LocalDate = LocalDate.MAX) {
        companion object {
            fun of(from: LocalDate?, to: LocalDate?) = when {
                from == null && to == null -> null
                else -> DateRange(from ?: LocalDate.MIN, to ?: LocalDate.MAX)
            }
        }

        fun createFilter(attributeName: String) = createANDFilter(
                // TODO handle unset dates
                createGreaterOrEqualFilter(attributeName, from.format(DateTimeFormatter.ISO_DATE)),
                createLessOrEqualFilter(attributeName, to.format(DateTimeFormatter.ISO_DATE))
        )
    }
}
