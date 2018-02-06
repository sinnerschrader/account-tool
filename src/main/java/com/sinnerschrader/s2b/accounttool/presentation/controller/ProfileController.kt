package com.sinnerschrader.s2b.accounttool.presentation.controller

import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import com.sinnerschrader.s2b.accounttool.presentation.model.ChangeProfile
import com.sinnerschrader.s2b.accounttool.presentation.validation.ChangeProfileFormValidator
import com.unboundid.ldap.sdk.LDAPConnection
import org.apache.catalina.servlet4preview.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.support.RedirectAttributes

import javax.annotation.Resource

@Controller
class ProfileController {

    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var mailService: MailService

    @Resource(name = "changeProfileFormValidator")
    private val changeProfileFormValidator: ChangeProfileFormValidator? = null

    @RequestMapping(path = arrayOf("/profile"), method = arrayOf(RequestMethod.GET))
    fun profile(request: HttpServletRequest, model: Model): ModelAndView {
        val connection = RequestUtils.getLdapConnection(request)
        val details = RequestUtils.getCurrentUserDetails()
        val mav = ModelAndView("pages/profile/index.html")
        if (details != null) {
            val user = ldapService.getUserByUid(connection, details.username)
            mav.addAllObjects(model.asMap())
            mav.addObject("user", user)
            mav.addObject("groups", ldapService.getGroupsByUser(connection, details.uid, details.dn))
            if (!model.containsAttribute(FORMNAME)) {
                model.addAttribute(FORMNAME, ChangeProfile(user!!))
            }
        }
        return mav
    }

    @RequestMapping(value = "/profile/change", method = arrayOf(RequestMethod.POST))
    @Throws(BusinessException::class)
    fun changeCurrentUserAttributes(
            request: HttpServletRequest,
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            @ModelAttribute(name = FORMNAME) form: ChangeProfile,
            attr: RedirectAttributes,
            bindingResult: BindingResult): String {
        val details = RequestUtils.getCurrentUserDetails()
        changeProfileFormValidator!!.validate(form, bindingResult)
        if (bindingResult.hasErrors()) {
            attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME, bindingResult)
            attr.addFlashAttribute(FORMNAME, form)
        } else {
            try {
                val ldapUser = ldapService.getUserByUid(connection, details!!.uid)
                val updatedUser = form.createUserEntityFromForm(ldapUser!!)
                if (form.isPasswordChange()) {
                    val res = ldapService.changePassword(connection, details, form.password)
                    val state = if (res) "sucess" else "failure"
                    log.info("{} changed his/her password", details.uid)
                    mailService.sendMailForAccountChange(ldapUser, "passwordChanged")
                } else {
                    ldapService.update(connection, updatedUser)
                    log.info("{} updated his/her account informations", details.uid)
                    if (form.isPublicKeyChange()) {
                        mailService.sendMailForAccountChange(ldapUser, "sshKeyUpdated")
                    }
                }
            } catch (be: BusinessException) {
                bindingResult.reject(be.code, be.args, "Could not update profile")
                attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME, bindingResult)
                attr.addFlashAttribute(FORMNAME, form)
            }

        }
        return "redirect:/profile"
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProfileController::class.java)
        private const val FORMNAME = "changeProfileForm"
    }
}
