package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.UserInfo
import com.unboundid.ldap.sdk.Filter.createANDFilter
import com.unboundid.ldap.sdk.Filter.createEqualityFilter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.SearchScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

interface CachedLdapService {
    fun getGroupMember(connection: LDAPConnection, uid: String): UserInfo?
}

@Service
class CachedLdapServiceImpl : CachedLdapService {
    companion object {
        private val LOG = LoggerFactory.getLogger(CachedLdapService::class.java)
    }

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Cacheable("groupMembers", key = "#uid", unless = "#result == null")
    override fun getGroupMember(connection: LDAPConnection, uid: String): UserInfo? {
        try {
            val searchResult = connection.search(
                ldapConfiguration.config.baseDN,
                SearchScope.SUB,
                createANDFilter(
                    createEqualityFilter("objectclass", "posixAccount"),
                    createEqualityFilter("uid", uid)
                ),
                "uid", "givenName", "sn"
            )

            return when (searchResult.searchEntries.size) {
                0 -> UserInfo(uid, "UNKNOWN", "UNKNOWN", "UNKNOWN")
                1 -> with(searchResult.searchEntries.first()) {
                    UserInfo(
                        uid = getAttributeValue("uid"),
                        givenName = getAttributeValue("givenName"),
                        sn = getAttributeValue("sn"),
                        o = companyForDn(dn))
                }
                else -> throw IllegalStateException()
            }
        } catch (e: Exception) {
            LOG.error("Could retrieve user [uid: $uid]", e)
            return null
        }
    }

    private fun companyForDn(dn: String) =
        try {
            with(Regex(",ou=([^,]+)").findAll(dn).last().groupValues[1]) {
                ldapConfiguration.companies[this] ?: "UNKNOWN"
            }
        } catch (e: NoSuchElementException) {
            "UNKNOWN"
        }
}