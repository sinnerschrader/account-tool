package com.sinnerschrader.s2b.accounttool.config.authentication

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import javax.servlet.http.HttpServletRequest


class LdapAuthenticationDetailsSource(private val ldapConfiguration: LdapConfiguration) : WebAuthenticationDetailsSource() {
    override fun buildDetails(context: HttpServletRequest) = LdapAuthenticationDetails(ldapConfiguration, context)
}
