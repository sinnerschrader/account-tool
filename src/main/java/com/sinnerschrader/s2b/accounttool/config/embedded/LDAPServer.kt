package com.sinnerschrader.s2b.accounttool.config.embedded

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.sdk.schema.Schema
import com.unboundid.ldif.LDIFReader
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.net.InetAddress
import javax.security.auth.DestroyFailedException
import javax.security.auth.Destroyable


/**
 * Custom embedded LDAP Server for development purpose. This embedded LDAP server will be started on development profile
 * automatically. For production profile this spring bean will be skipped.
 */
@Service("ldapServer")
@Profile("development", "test")
class LDAPServer(
    private val ldapConfiguration: LdapConfiguration
) : ApplicationContextAware, InitializingBean, Destroyable {
    private lateinit var directoryServer: InMemoryDirectoryServer

    @Value("\${ldap.embedded:false}")
    private var enabled = false

    private var applicationContext: ApplicationContext? = null

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (!enabled)
            return

        LOG.info("Starting embedded LDAP Server")
        val host = InetAddress.getByName(ldapConfiguration.host)
        val config = InMemoryDirectoryServerConfig("cn=config", ldapConfiguration.baseDN)
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", host, ldapConfiguration.port, null))

        val schemaLdifs = arrayOf(
            "classpath:ldap/schema/01-system.ldif",
            "classpath:ldap/schema/02-core.ldif",
            "classpath:ldap/schema/03-cosine.ldif",
            "classpath:ldap/schema/04-nis.ldif",
            "classpath:ldap/schema/05-inetOrgPerson.ldif",
            "classpath:ldap/schema/06-ppolicy.ldif",
            "classpath:ldap/schema/07-samba.ldif",
            "classpath:ldap/schema/08-szz.ldif")

        val customSchemas = schemaLdifs.map {
            LOG.debug("Loading Schema from File: {}", it)
            applicationContext!!.getResource(it).inputStream.use {
                Schema.getSchema(it)
            }
        }

        config.schema = Schema.mergeSchemas(Schema.getDefaultStandardSchema(), *customSchemas.toTypedArray())
        LOG.info("Schema was initialized, loading data next")

        directoryServer = InMemoryDirectoryServer(config)
        val dataLdifs = arrayOf(
            "classpath:ldap/data/01-company-structure.ldif",
            "classpath:ldap/data/02-groups.ldif",
            "classpath:ldap/data/03-testuser.ldif")

        for (ldif in dataLdifs) {
            val ldifResource = applicationContext!!.getResource(ldif)
            LOG.debug("Loading Data from File: {}", ldifResource.filename)
            ldifResource.inputStream.use { directoryServer.importFromLDIF(false, LDIFReader(it)) }
        }

        directoryServer.startListening()
        LOG.info("Started embedded LDAP Service on {}", "ldap://${host.hostAddress}:${ldapConfiguration.port}")
    }

    @Throws(DestroyFailedException::class)
    override fun destroy() {
        directoryServer.shutDown(true)
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LDAPServer::class.java)
    }

}
