package com.sinnerschrader.s2b.accounttool.presentation.interceptor

import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Interceptor for displaying messages from application to the user. The messages have a
 * validity to the next request (next GET request).
 */
class GlobalMessageInterceptor(private val globalMessageFactory: GlobalMessageFactory) : HandlerInterceptorAdapter() {
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method.equals("get", ignoreCase = true))
            request.setAttribute("globalMessages", globalMessageFactory.pop(request))
        return true
    }
}
