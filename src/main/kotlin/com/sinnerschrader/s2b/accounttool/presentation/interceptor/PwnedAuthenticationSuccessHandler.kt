package com.sinnerschrader.s2b.accounttool.presentation.interceptor

import com.sinnerschrader.s2b.accounttool.logic.component.security.PwnedPasswordService
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.currentUserDetails
import org.springframework.security.core.Authentication
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object PwnedAuthenticationSuccessHandler : SavedRequestAwareAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(request: HttpServletRequest,
                                         response: HttpServletResponse,
                                         authentication: Authentication) =
            if (PwnedPasswordService.isPwned(currentUserDetails!!.password!!))
                DefaultRedirectStrategy().sendRedirect(request, response, "/pwned")
            else super.onAuthenticationSuccess(request, response, authentication)
}