package com.sinnerschrader.s2b.accounttool.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import org.apache.catalina.servlet4preview.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

import java.io.StringWriter

@Controller
class MainController {

    @RequestMapping(path = ["/"])
    fun root() = "redirect:/profile"

    @RequestMapping("/login", method = [RequestMethod.GET])
    fun login(
            request: HttpServletRequest,
            @RequestParam(value = "error", required = false) error: String?,
            @RequestParam(value = "logout", required = false) logout: String?) =
            when {
                RequestUtils.currentUserDetails != null -> ModelAndView("redirect:/profile")
                else -> ModelAndView("pages/login.html").apply {
                    error?.let { addObject("error", getErrorCode(request)) }
                    logout?.let { addObject("msg", "logout.success") }
                }
            }

    @RequestMapping("/pwned", method = [RequestMethod.GET])
    fun pwned() = ModelAndView("pages/security/pwned.html")

    //customize the error message
    private fun getErrorCode(request: HttpServletRequest) =
            when (request.session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION") as Exception) {
                is BadCredentialsException -> "login.invalid.credentials"
                is DisabledException -> "login.account.disabled"
                else -> "login.general.error"
            }

    @RequestMapping("/logout", method = [RequestMethod.GET])
    fun logout(request: HttpServletRequest): String {
        RequestUtils.currentUserDetails?.let {
            LOG.debug("{} has been successfully logged off", it.uid)
        }
        request.session.invalidate()
        return "redirect:/login?logout"
    }

    fun logCspReport(cspReportString: String) {
        try {
            val writer = StringWriter()
            with(ObjectMapper()) {
                enable(SerializationFeature.INDENT_OUTPUT)
                writeValue(writer, readTree(cspReportString))
            }
            LOG.warn("CSP-Report: \n{}", writer.toString())
        } catch (e: Exception) {
            // ignore exception and print request body plain
            LOG.warn("CSP-Report: {}", cspReportString)
        }
    }

    @RequestMapping("/csp-report", method = [RequestMethod.POST])
    @ResponseBody
    fun cspReport(@RequestBody(required = false) cspReportString: String?): ResponseEntity<String> =
            when {
                cspReportString?.apply { logCspReport(this) } != null -> ResponseEntity(HttpStatus.OK)
                else -> ResponseEntity(HttpStatus.NOT_FOUND)
            }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainController::class.java)
    }
}
