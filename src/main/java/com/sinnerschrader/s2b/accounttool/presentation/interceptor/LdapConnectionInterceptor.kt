package com.sinnerschrader.s2b.accounttool.presentation.interceptor

import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.*
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.ResultCode
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class LdapConnectionInterceptor(private val ldapConfiguration: LdapConfiguration) : HandlerInterceptorAdapter() {

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        getCurrentUserDetails()?.apply {
            try {
                ldapConfiguration.createConnection().use {
                    val bindResult = it.bind(this.dn, this.password)
                    if(bindResult.resultCode == ResultCode.SUCCESS)
                        setLdapConnection(request, it)
                }
            } catch (le: LDAPException) {
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
        with(getCurrentUserDetails()){
            if (this != null && modelAndView != null) {
                modelAndView.addObject("currentUser", this)
            }
        }
    }
}
