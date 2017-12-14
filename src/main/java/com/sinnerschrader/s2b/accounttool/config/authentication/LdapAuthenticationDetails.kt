package com.sinnerschrader.s2b.accounttool.config.authentication

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import org.springframework.security.web.authentication.WebAuthenticationDetails

import javax.servlet.http.HttpServletRequest


class LdapAuthenticationDetails
internal constructor(ldapConfiguration: LdapConfiguration,
                     request: HttpServletRequest) : WebAuthenticationDetails(request) {
    val userDN: String
    val username = request.getParameter("uid")
    val password = request.getParameter("password")
    val company = request.getParameter("company")

    init { this.userDN = ldapConfiguration.getUserBind(username, company) }
}
