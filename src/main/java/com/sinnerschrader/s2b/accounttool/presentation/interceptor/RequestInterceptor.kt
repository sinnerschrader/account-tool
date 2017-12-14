package com.sinnerschrader.s2b.accounttool.presentation.interceptor

import org.springframework.core.env.Environment
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * A simple Request Interceptor which provides some global Objects / Informations
 */
class RequestInterceptor(private val environment: Environment) : HandlerInterceptorAdapter() {

    @Throws(Exception::class)
    override fun postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any,
                            modelAndView: ModelAndView?) {
        if (request.method.equals("GET", ignoreCase = true) &&
            modelAndView != null &&
            modelAndView.viewName.startsWith("redirect:")) {
            modelAndView.addObject("env", environment)
        }
    }
}
