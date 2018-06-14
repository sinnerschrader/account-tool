package com.sinnerschrader.s2b.accounttool.config.ldap

import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.util.ssl.SSLUtil
import com.unboundid.util.ssl.TrustAllTrustManager
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.security.GeneralSecurityException
import java.text.MessageFormat

@Configuration
@ConfigurationProperties(prefix = "ldap")
class LdapConfiguration {
    var config = LdapBaseConfig()
    var permissions = LdapPermissions()
    var groupPrefixes = LdapGroupPrefixes()
    var companies = mutableMapOf<String,String>()

    @Throws(LDAPException::class, GeneralSecurityException::class)
    fun createConnection(): LDAPConnection = LDAPConnection(
        if (config.isSsl) SSLUtil(TrustAllTrustManager()).createSSLSocketFactory() else null,
        config.host, config.port)

    fun getUserBind(uid: String, companyKey: String): String =
        with(config.userDN[companyKey]!!) {
            when {
                !isEmpty() -> MessageFormat.format(this, uid)
                else -> throw IllegalArgumentException("The provided company key '$companyKey'is not allowed or known")
            }
        }

    class LdapPermissions {
        var ldapAdminGroup = ""
        var userAdminGroups = mutableListOf<String>()
        var defaultGroups = mutableMapOf<String, List<String>>()
    }

    class LdapGroupPrefixes {
        var admin = ""
        var team = ""
        var technical = ""
    }

    class LdapBaseConfig {
        var host = ""
        var port: Int = 0
        var isSsl: Boolean = false
        var dc = ""
        var baseDN = ""
        var groupDN = ""
        var userDN = mutableMapOf<String,String>()
    }
}
