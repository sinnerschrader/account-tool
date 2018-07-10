package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.logic.entity.UserInfo
import com.unboundid.ldap.sdk.Filter.createANDFilter
import com.unboundid.ldap.sdk.Filter.createEqualityFilter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.SearchScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import javax.management.timer.Timer.ONE_DAY

interface CachedLdapService {
    fun clearCacheGroupMembers()
    fun getGroupMember(connection: LDAPConnection, uid: String): UserInfo?
    fun companyForDn(dn: String): String
}

@Service
class CachedLdapServiceImpl : CachedLdapService {
    companion object {
        private val LOG = LoggerFactory.getLogger(CachedLdapService::class.java)
    }

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Scheduled(fixedRate = ONE_DAY)
    @CacheEvict("groupMembers")
    override fun clearCacheGroupMembers() = Unit

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
                    "uid", "givenName", "sn", "mail", "szzStatus"
            )

            return when (searchResult.searchEntries.size) {
                0 -> UserInfo("UNKNOWN", uid, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", User.State.inactive)
                1 -> with(searchResult.searchEntries.first()) {
                    UserInfo(
                            dn = dn,
                            uid = getAttributeValue("uid"),
                            givenName = getAttributeValue("givenName"),
                            sn = getAttributeValue("sn"),
                            o = companyForDn(dn),
                            mail = getAttributeValue("mail"),
                            szzStatus = User.State.valueOf(getAttributeValue("szzStatus")))
                }
                else -> throw IllegalStateException()
            }
        } catch (e: Exception) {
            LOG.error("Could retrieve user [uid: $uid]", e)
            return null
        }
    }

    override fun companyForDn(dn: String) =
            try {
                with(Regex(",ou=([^,]+)").findAll(dn).last().groupValues[1]) {
                    ldapConfiguration.companies[this] ?: "UNKNOWN"
                }
            } catch (e: NoSuchElementException) {
                "UNKNOWN"
            }
}