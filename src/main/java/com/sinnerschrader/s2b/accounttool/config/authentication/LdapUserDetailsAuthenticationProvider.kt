package com.sinnerschrader.s2b.accounttool.config.authentication

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.LogService
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.ResultCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*


@Component
class LdapUserDetailsAuthenticationProvider : AbstractUserDetailsAuthenticationProvider() {

    @Autowired
    private lateinit var logService: LogService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var ldapService: LdapService

    @Throws(AuthenticationException::class)
    override fun retrieveUser(username: String, authentication: UsernamePasswordAuthenticationToken): UserDetails {
        val details = authentication.details as LdapAuthenticationDetails
        ldapConfiguration.createConnection().use {
            try {
                val resultCode = it.bind(details.userDN, details.password).resultCode
                when (resultCode) {
                    ResultCode.SUCCESS -> return createUserDetails(it, details, retrieveGroups(it, details))
                    else -> throw LDAPException(resultCode)
                }
            } catch (e: Exception) {
                log.warn("Authentication failed [uid: ${details.username}]: ${e.message}")
                throw when (e) {
                    is AuthenticationException -> e
                    is LDAPException -> BadCredentialsException(e.resultCode.name, e)
                    else -> AuthenticationServiceException(e.message, e)
                }
            }
        }
    }

    @Throws(AuthenticationException::class)
    private fun createUserDetails(connection: LDAPConnection, details: LdapAuthenticationDetails,
                                  groups: List<GrantedAuthority>): LdapUserDetails {
        val currentUser = ldapService.getUserByUid(connection, details.username)
        if (currentUser!!.szzStatus !== User.State.active) {
            throw DisabledException("Inactive accounts can't login")
        }
        log.info("User ${currentUser!!.uid} logged in successful")
        return LdapUserDetails(
            details.userDN,
            details.username,
            currentUser.displayName!!,
            details.password,
            details.company,
            groups,
            currentUser.szzStatus !== User.State.active,
            currentUser.szzStatus === User.State.active)
    }

    @Throws(AuthenticationException::class)
    private fun retrieveGroups(connection: LDAPConnection, details: LdapAuthenticationDetails): List<GrantedAuthority> {
        val groups = LinkedList<GrantedAuthority>()
        ldapService.getGroupsByUser(connection, details.username, details.userDN)
            .forEach { group -> groups.add(SimpleGrantedAuthority(group.cn)) }
        return groups
    }

    @Throws(AuthenticationException::class)
    override fun additionalAuthenticationChecks(userDetails: UserDetails,
                                                authentication: UsernamePasswordAuthenticationToken) {
        // nothing to do here
    }

    companion object {
        private val log = LoggerFactory.getLogger(LdapUserDetailsAuthenticationProvider::class.java)
    }

}
