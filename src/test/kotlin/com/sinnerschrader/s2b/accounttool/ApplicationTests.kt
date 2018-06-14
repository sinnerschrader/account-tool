package com.sinnerschrader.s2b.accounttool

import com.sinnerschrader.s2b.accounttool.config.embedded.LDAPServer
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.Assert


@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun contextLoads() {
        Assert.isTrue(applicationContext.environment.acceptsProfiles("test"), "\"test\" profile was not loaded")
        Assert.notNull(applicationContext.getBean(AuthorizationService::class.java), "Missing AuthorizationService")
        Assert.notNull(applicationContext.getBean(LDAPServer::class.java), "Missing embedded LDAPServer")
        Assert.notNull(applicationContext.getBean(LdapService::class.java), "Missing LdapService")

        Assert.notNull(applicationContext.getBean("ldapServer"), "Missing Bean by name ldapServer")
        Assert.notNull(applicationContext.getBean("ldapConfiguration"), "Missing Bean by name ldapConfiguration")
        Assert.notNull(applicationContext.getBean("groupMapping"), "Missing Bean by name groupMapping")
        Assert.notNull(applicationContext.getBean("userMapping"), "Missing Bean by name userMapping")
    }
}
