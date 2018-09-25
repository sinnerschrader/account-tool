package com.sinnerschrader.s2b.accounttool.presentation.controller

import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.logic.component.mail.AccountChangeMail
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import com.sinnerschrader.s2b.accounttool.presentation.controller.ProfileController.Edit.*
import com.sinnerschrader.s2b.accounttool.presentation.model.ChangeProfile
import com.sinnerschrader.s2b.accounttool.presentation.validation.ChangeProfileFormValidator
import com.unboundid.ldap.sdk.LDAPConnection
import org.apache.catalina.servlet4preview.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
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

    enum class Edit { PASSWORD, PHONE, SSH_KEY, EXTERNAL_ACCOUNTS, NONE }

    @RequestMapping("/profile", method = [GET])
    fun profile(request: HttpServletRequest, model: Model,
                @RequestParam("edit", required = false, defaultValue = "NONE") edit: Edit): ModelAndView {
        val connection = RequestUtils.getLdapConnection(request)!!
        val details = RequestUtils.currentUserDetails
        val mav = ModelAndView("pages/profile/index.html")
        if (details != null) {
            val user = ldapService.getUserByUid(connection, details.username, skipCache = true)!!
            mav.addAllObjects(model.asMap())
            mav.addObject("user", user)
            mav.addObject("groups", ldapService.getGroupsByUser(connection, details.uid, details.dn))
            mav.addObject("edit", edit)
            if (!model.containsAttribute(FORMNAME)) {
                model.addAttribute(FORMNAME, ChangeProfile(user!!))
            }
        }
        return mav
    }

    @RequestMapping("/profile/change", method = [POST])
    @Throws(BusinessException::class)
    fun changeCurrentUserAttributes(
            request: HttpServletRequest,
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            @ModelAttribute(name = FORMNAME) form: ChangeProfile,
            attr: RedirectAttributes,
            bindingResult: BindingResult): String {
        val details = RequestUtils.currentUserDetails
        changeProfileFormValidator!!.validate(form, bindingResult)
        if (bindingResult.hasErrors()) {
            attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME, bindingResult)
            attr.addFlashAttribute(FORMNAME, form)
        } else {
            try {
                val ldapUser = ldapService.getUserByUid(connection, details!!.uid, skipCache = true)!!
                val updatedUser = with(form){
                    when(edit){
                        PHONE -> ldapUser.copy(mobile = mobile.trim(), telephoneNumber = telephone.trim())
                        SSH_KEY -> ldapUser.copy(szzPublicKey = publicKey.trim())
                        EXTERNAL_ACCOUNTS -> ldapUser.copy(szzExternalAccounts = szzExternalAccounts)
                        else -> ldapUser
                    }
                }

                if (form.edit == PASSWORD) {
                    ldapService.changePassword(connection, details, form.password)
                    log.info("{} changed his/her password", details.uid)
                    mailService.sendMail(listOf(ldapUser), AccountChangeMail(ldapUser, AccountChangeMail.Action.PASSWORD_CHANGED))
                } else {
                    ldapService.update(connection, updatedUser)
                    log.info("{} updated his/her account informations", details.uid)
                    if (form.edit == SSH_KEY) {
                        mailService.sendMail(listOf(ldapUser), AccountChangeMail(ldapUser, AccountChangeMail.Action.SSH_KEY_CHANGED))
                    }
                }
            } catch (be: BusinessException) {
                bindingResult.reject(be.code, be.args, "Could not update profile")
                attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME, bindingResult)
                attr.addFlashAttribute(FORMNAME, form)
            }
        }

        val edit = with(form) {
            when {
                !bindingResult.hasErrors() -> NONE
                else -> edit
            }
        }
        return "redirect:/profile?edit=$edit"
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProfileController::class.java)
        private const val FORMNAME = "changeProfileForm"
    }
}
