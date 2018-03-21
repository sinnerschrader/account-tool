package com.sinnerschrader.s2b.accounttool.presentation

import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.unboundid.ldap.sdk.LDAPConnection
import org.springframework.security.core.context.SecurityContextHolder
import javax.servlet.http.HttpServletRequest


object RequestUtils {
    val currentUserDetails: LdapUserDetails?
        get() {
            val auth = SecurityContextHolder.getContext().authentication
            return if (auth != null && auth.isAuthenticated && "anonymousUser" != auth.name) {
                auth.principal as LdapUserDetails
            } else null
        }

    fun getLdapConnection(request: HttpServletRequest) =
            request.getAttribute(WebConstants.ATTR_CONNECTION) as LDAPConnection?

    fun setLdapConnection(request: HttpServletRequest, connection: LDAPConnection) {
        request.setAttribute(WebConstants.ATTR_CONNECTION, connection)
    }
}
