package com.sinnerschrader.s2b.accounttool.presentation.interceptor

import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.currentUserDetails
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.getLdapConnection
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.setLdapConnection
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.ResultCode.SUCCESS
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class LdapConnectionInterceptor(private val ldapConfiguration: LdapConfiguration) : HandlerInterceptorAdapter() {

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        currentUserDetails?.apply {
            var connection: LDAPConnection? = null
            try {
                connection = ldapConfiguration.createConnection()
                with(connection.bind(dn, password)) {
                    if (resultCode === SUCCESS) setLdapConnection(request, connection)
                }
            } catch (le: LDAPException) {
                connection?.close()
                request.session.invalidate()
                return false
            }
        }
        return true
    }

    @Throws(Exception::class)
    override fun postHandle(request: HttpServletRequest, response: HttpServletResponse,
                            handler: Any, modelAndView: ModelAndView?) {
        getLdapConnection(request)?.close()
        request.removeAttribute(WebConstants.ATTR_CONNECTION)
        currentUserDetails?.let {
            modelAndView?.addObject("currentUser", it)
        }
    }
}
