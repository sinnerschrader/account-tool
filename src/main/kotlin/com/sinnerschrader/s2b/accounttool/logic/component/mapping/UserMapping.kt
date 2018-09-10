package com.sinnerschrader.s2b.accounttool.logic.component.mapping

import com.sinnerschrader.s2b.accounttool.config.UserConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.unboundid.ldap.sdk.SearchResultEntry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class UserMapping {

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var userConfiguration: UserConfiguration

    fun map(entry: SearchResultEntry?): User? {
        if (entry == null) return null

        fun SearchResultEntry.int(attributeName: String) = getAttributeValueAsInteger(attributeName)
        fun SearchResultEntry.long(attributeName: String) = getAttributeValueAsLong(attributeName)
        fun SearchResultEntry.str(attributeName: String) = getAttributeValue(attributeName)
        fun SearchResultEntry.strMap(attributeName: String) =
                if (hasAttribute(attributeName)) str(attributeName).split(',')
                    .map { it.split('=', limit = 2).let { valuePair -> valuePair[0] to valuePair[1] } }.toMap()
                else emptyMap()

        return try {
            with(entry) {
                User(
                        dn = dn,
                        uid = str("uid"),
                        uidNumber = int("uidNumber"),
                        gidNumber = int("gidNumber"),
                        givenName = str("givenName"),
                        sn = str("sn"),
                        displayName = str("displayName"),
                        gecos = str("gecos"),
                        cn = str("cn"),
                        homeDirectory = str("homeDirectory"),
                        loginShell = str("loginShell"),
                        szzBirthDay = int("szzBirthDay"),
                        szzBirthMonth = int("szzBirthMonth"),
                        sambaSID = str("sambaSID"),
                        sambaPasswordHistory = str("sambaPasswordHistory"),
                        sambaAcctFlags = str("sambaAcctFlags"),
                        mail = str("mail"),
                        szzStatus = User.State.fromString(str("szzStatus")),
                        szzMailStatus = User.State.fromString(str("szzMailStatus")),
                        sambaPwdLastSet = long("sambaPwdLastSet") ?: 0L,
                        szzEntryDate = parseDate(dn, str("szzEntryDate")),
                        szzExitDate = parseDate(dn, str("szzExitDate")),
                        ou = str("ou"),
                        description = str("description"),
                        telephoneNumber = str("telephoneNumber") ?: "",
                        mobile = str("mobile") ?: "",
                        employeeNumber = str("employeeNumber"),
                        title = str("title") ?: "",
                        l = str("l"),
                        szzPublicKey = str("szzPublicKey") ?: "",
                        szzExternalAccounts = userConfiguration.externalAccounts.keys.map { it to "" }.toMap() + strMap("szzExternalAccounts"),
                        o = companyForDn(dn).second,
                        companyKey = companyForDn(dn).first,
                        modifiersName = str("modifiersName"),
                        modifytimestamp = str("modifytimestamp")
                )
            }
        } catch (e: Exception) {
            LOG.error("failed to map: " + entry.dn, e)
            null
        }
    }

    private fun companyForDn(dn: String) =
            try {
                with(Regex(",ou=([^,]+)").findAll(dn).last().groupValues[1]) {
                    this to (ldapConfiguration.companies[this] ?: "UNKNOWN")
                }
            } catch (e: NoSuchElementException) {
                "UNKNOWN" to "UNKNOWN"
            }


    private fun parseDate(dn: String, date: String?, required: Boolean = true): LocalDate? =
            date?.let {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    if (required)
                        LOG.warn("Could not parse date [dn=$dn, date=$date]")
                    null
                }
            }

    private fun parseDate(dn: String, required: Boolean, year: Int?, month: Int?, day: Int?): LocalDate? =
            try {
                LocalDate.of(year!!, month!!, day!!)
            } catch (e: Exception) {
                if (required)
                    LOG.warn("Could not parse date [dn=$dn, required=$required, year=$year, month=$month, day=$day]")
                null
            }

    companion object {
        private val LOG = LoggerFactory.getLogger(UserMapping::class.java)
    }
}
