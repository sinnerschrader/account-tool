package com.sinnerschrader.s2b.accounttool.presentation.controller

import com.sinnerschrader.s2b.accounttool.config.DomainConfiguration
import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapBusinessService
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory
import com.sinnerschrader.s2b.accounttool.presentation.model.UserForm
import com.sinnerschrader.s2b.accounttool.presentation.validation.UserFormValidator
import com.unboundid.ldap.sdk.LDAPConnection
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@Controller
class UserController {

    @Resource(name = "userFormValidator")
    private lateinit var userFormValidator: UserFormValidator

    @Autowired
    private lateinit var authorizationService: AuthorizationService

    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var mailService: MailService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var domainConfiguration: DomainConfiguration

    @Autowired
    private lateinit var globalMessageFactory: GlobalMessageFactory

    @Autowired
    private lateinit var ldapBusinessService: LdapBusinessService

    @RequestMapping("/user/maintenance", method = [GET])
    fun maintenance(): ModelAndView {
        authorizationService.ensureUserAdministration(RequestUtils.currentUserDetails!!)

        val mav = ModelAndView("pages/user/maintenance.html")
        mav.addObject("leavingUsers", ldapBusinessService.leavingUsers())
        mav.addObject("unmaintainedUsers", ldapBusinessService.unmaintainedExternals())
        mav.addObject("unmaintainedMailUsers", ldapBusinessService.unmaintainedMailUsers())
        return mav
    }

    @RequestMapping("/user", method = [GET])
    fun searchUser(
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            @RequestParam(name = "searchTerm", required = false, defaultValue = "") searchTerm: String): ModelAndView {
        authorizationService.ensureUserAdministration(RequestUtils.currentUserDetails!!)

        val users = if (searchTerm.isNotBlank()) ldapService.findUserBySearchTerm(connection, searchTerm) else emptySet()

        return ModelAndView("pages/user/index.html").apply {
            addObject("company", ldapConfiguration.companies)
            addObject("users", users)
        }
    }

    @RequestMapping("/user/edit/{userId}", method = [GET])
    fun edit(
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            @PathVariable(name = "userId") userId: String,
            model: Model): ModelAndView {
        val details = RequestUtils.currentUserDetails
        authorizationService.ensureUserAdministration(details!!)

        val user = ldapService.getUserByUid(connection, userId)
        if (StringUtils.isBlank(userId) || user == null) {
            return ModelAndView("redirect:/user")
        }
        if (!model.containsAttribute(FORMNAME_EDIT)) {
            model.addAttribute(FORMNAME_EDIT, UserForm(user))
        }
        val mav = ModelAndView()
        mav.addAllObjects(model.asMap())
        mav.addObject("user", user)
        mav.addObject("companies", ldapConfiguration.companies)
        mav.addObject("types", ldapService.getEmployeeType(connection))
        mav.addObject("departments", ldapService.getDepartments(connection))
        mav.addObject("locations", ldapService.getLocations(connection))
        mav.addObject("groups", ldapService.getGroupsByUser(connection, user.uid, user.dn))
        mav.viewName = "pages/user/edit.html"
        return mav
    }

    @RequestMapping("/user/edit/{userId}", method = [POST])
    fun performEdit(
            request: HttpServletRequest,
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            @PathVariable(name = "userId") userId: String,
            @ModelAttribute(FORMNAME_EDIT) @Valid userForm: UserForm,
            attr: RedirectAttributes,
            bindingResult: BindingResult): String {
        val details = RequestUtils.currentUserDetails
        authorizationService.ensureUserAdministration(details!!)

        // half security check, if readonly field was manipulated
        if (!StringUtils.equals(userForm.uid, userId)) {
            throw IllegalArgumentException("Form submit was modified")
        }

        userFormValidator.validate(userForm, bindingResult)
        if (bindingResult.hasErrors()) {
            attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME_EDIT, bindingResult)
            attr.addFlashAttribute(FORMNAME_EDIT, userForm)
            return "redirect:/user/edit/$userId"
        }
        try {
            if (isNotBlank(userForm.save)) {
                val currentUser = ldapService.getUserByUid(connection, userId)
                val changedUser = ldapService.update(connection, userForm.createUserEntityFromForm(ldapConfiguration, domainConfiguration))

                globalMessageFactory.store(request,
                        globalMessageFactory.createInfo("user.edit.success", changedUser!!.uid))
                log.info("{} updated the account of user {}: {}", details.uid, currentUser!!.uid, currentUser.diff(changedUser))
            }
            if (isNotBlank(userForm.resetPassword) || isNotBlank(userForm.activateUser) || isNotBlank(userForm.deactivateUser)) {
                val hidePassword = false
                val user = ldapService.getUserByUid(connection, userId)
                val password = ldapService.resetPassword(connection, user!!)!!
                // update current user details for correct bind on next request
                if (StringUtils.equals(details.uid, user.uid)) {
                    details.setPassword(password)
                }
                if (isNotBlank(userForm.resetPassword)) {
                    log.info("{} reseted the password of user {}", details.uid, user.uid)
                }
                var message = if (hidePassword) "user.passwordReset.simple" else "user.passwordReset.full"
                if (isNotBlank(userForm.activateUser)) {
                    message = "user.activated"
                    ldapService.changeUserState(connection, user.uid, User.State.active)
                    log.info("{} activated the user {} right now", details.uid, user.uid)
                }
                if (isNotBlank(userForm.deactivateUser)) {
                    message = "user.deactivated"
                    ldapService.changeUserState(connection, user.uid, User.State.inactive)
                    log.info("{} deactivated the user {} right now", details.uid, user.uid)
                }
                globalMessageFactory.store(request,
                        globalMessageFactory.createInfo(message, user.uid, password))
                return "redirect:/user/edit/$userId"
            }
        } catch (be: BusinessException) {
            bindingResult.reject(be.code, be.args, "Could not update user")
            attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME_EDIT, bindingResult)
            attr.addFlashAttribute(FORMNAME_EDIT, userForm)
            return "redirect:/user/edit/$userId"
        }

        return "redirect:/user"
    }

    @RequestMapping("/user/create", method = [GET])
    fun create(
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            model: Model): ModelAndView {
        val details = RequestUtils.currentUserDetails
        authorizationService.ensureUserAdministration(details!!)

        if (!model.containsAttribute(FORMNAME_CREATE)) {
            model.addAttribute(FORMNAME_CREATE, UserForm(details))
        }
        val mav = ModelAndView()
        mav.addAllObjects(model.asMap())
        mav.addObject("companies", ldapConfiguration.companies)
        mav.addObject("types", ldapService.getEmployeeType(connection))
        mav.addObject("departments", ldapService.getDepartments(connection))
        mav.addObject("locations", ldapService.getLocations(connection))
        mav.viewName = "pages/user/create.html"
        return mav
    }

    @RequestMapping("/user/create", method = [POST])
    fun performCreate(
            request: HttpServletRequest,
            @RequestAttribute(name = WebConstants.ATTR_CONNECTION) connection: LDAPConnection,
            @ModelAttribute(FORMNAME_CREATE) @Valid userForm: UserForm,
            bindingResult: BindingResult, attr: RedirectAttributes): String {
        val currentUser = RequestUtils.currentUserDetails
        authorizationService.ensureUserAdministration(currentUser!!)

        userFormValidator.validate(userForm, bindingResult)
        if (bindingResult.hasErrors()) {
            attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME_CREATE, bindingResult)
            attr.addFlashAttribute(FORMNAME_CREATE, userForm)
            return "redirect:/user/create"
        }

        try {
            val newUser = ldapService.insert(connection, userForm.createUserEntityFromForm(ldapConfiguration, domainConfiguration))
            val password = ldapService.resetPassword(connection, newUser!!)!!
            ldapService.addDefaultGroups(newUser)
            globalMessageFactory.store(request,
                    globalMessageFactory.createInfo("user.create.success", newUser.uid, password))
            log.info("{} created a new account with uid {}", currentUser.uid, newUser.uid)
        } catch (be: BusinessException) {
            bindingResult.reject(be.code, be.args, "Could not create user")
            attr.addFlashAttribute(BindingResult::class.java.name + "." + FORMNAME_CREATE, bindingResult)
            attr.addFlashAttribute(FORMNAME_CREATE, userForm)
            return "redirect:/user/create"
        }

        return "redirect:/user"
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserController::class.java)
        private const val FORMNAME_CREATE = "createUserForm"
        private const val FORMNAME_EDIT = "editUserForm"
    }
}
