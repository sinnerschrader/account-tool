package com.sinnerschrader.s2b.accounttool.config.authentication

import org.springframework.security.web.authentication.WebAuthenticationDetails
import java.nio.charset.StandardCharsets
import java.util.*

import javax.servlet.http.HttpServletRequest


class LdapAuthenticationDetails
internal constructor(request: HttpServletRequest,
                     dn: (uid: String) -> String) : WebAuthenticationDetails(request) {
    val username = request.basicAuth()?.first ?: request.getParameter("uid")
    val password = request.basicAuth()?.second ?: request.getParameter("password")
    val userDN = dn.invoke(username)

    private fun HttpServletRequest.basicAuth() = try{
        with(getHeader("Authorization").substring("Basic ".length).trim()) {
            String(Base64.getDecoder().decode(this), StandardCharsets.UTF_8).split(":", limit=2).let {
                it[0] to it[1]
            }
        }
    } catch (e: java.lang.Exception) {
        null
    }
}
