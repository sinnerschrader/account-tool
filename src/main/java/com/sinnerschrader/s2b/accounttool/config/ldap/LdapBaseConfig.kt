package com.sinnerschrader.s2b.accounttool.config.ldap
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "ldap.config")
class LdapBaseConfig : InitializingBean {
    lateinit var host: String
    var port: Int = 0
    var isSsl: Boolean = false
    lateinit var dc: String
    lateinit var baseDN: String
    lateinit var groupDN: String
    var userDN: List<String> = mutableListOf()
    private var userDnMap: Map<String, String> = emptyMap()

    fun getUserDnByCompany(companyKey: String) = userDnMap[companyKey]!!

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        userDnMap = userDN.map {
            val (key, value) = it.split(':', limit = 2)
            key to value
        }.toMap()
    }
}