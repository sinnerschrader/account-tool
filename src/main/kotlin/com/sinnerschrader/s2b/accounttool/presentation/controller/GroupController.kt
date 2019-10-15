package com.sinnerschrader.s2b.accounttool.presentation.controller

import com.sinnerschrader.s2b.accounttool.config.WebConstants.ATTR_CONNECTION
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.logic.component.mail.GroupAccessRequestMail
import com.sinnerschrader.s2b.accounttool.logic.component.mail.GroupChangeMail
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.currentUserDetails
import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory
import com.unboundid.ldap.sdk.LDAPConnection
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest


@Controller
class GroupController {

    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var mailService: MailService

    @Autowired
    private lateinit var authorizationService: AuthorizationService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var ldapManagementConfiguration: LdapManagementConfiguration

    @Autowired
    private lateinit var globalMessageFactory: GlobalMessageFactory

    private fun groups(connection: LDAPConnection, listAllGroups: Boolean) =
            with(currentUserDetails ?: throw IllegalStateException("Invalid access on groups")) {
                if (listAllGroups) ldapService.getGroups(connection) else ldapService.getGroupsByUser(connection, uid, dn)
            }

    @RequestMapping("/group", method = [GET])
    fun listGroups(
            @RequestAttribute(ATTR_CONNECTION, required = true) connection: LDAPConnection,
            @RequestParam("all", defaultValue = "false", required = false) listAllGroups: Boolean) =
            ModelAndView("pages/group/index.html").apply {
                addObject("groups", groups(connection, listAllGroups))
                addObject("showAllGroups", listAllGroups)
            }

    @RequestMapping("/group/{groupCN}", method = [GET])
    fun viewGroup(
            @RequestAttribute(ATTR_CONNECTION) connection: LDAPConnection,
            @PathVariable("groupCN") groupCN: String,
            @RequestParam("messageKey", defaultValue = "", required = false) messageKey: String,
            @RequestParam("searchTerm", required = false, defaultValue = "") searchTerm: String,
            @RequestParam("all", defaultValue = "false", required = false) listAllGroups: Boolean): ModelAndView {

        val group = ldapService.getGroupByCN(connection, groupCN) ?: return ModelAndView("redirect:/group/$groupCN")
        val users = if (searchTerm.isNotBlank())
            ldapService.findUserBySearchTerm(connection, searchTerm).filter { !group.hasMember(it.uid, it.dn) }
        else emptyList()

        val selectedGroup = ldapService.getGroupByCN(connection, groupCN) ?: return ModelAndView("redirect:/group")
        val usersByGroup = ldapService.getGroupMembers(connection, selectedGroup)

        return ModelAndView("pages/group/index.html").apply {
            addObject("company", ldapConfiguration.companies)
            addObject("messageKey", messageKey)
            addObject("group", group)
            addObject("groups", groups(connection, listAllGroups))
            addObject("selectedGroup", selectedGroup)
            addObject("usersByGroup", usersByGroup)
            addObject("users", users)
            addObject("showAllGroups", listAllGroups)
        }
    }

    @RequestMapping("/group/{groupCN}/adduser/{uid}", method = [POST])
    fun addUserToGroup(
            request: HttpServletRequest,
            @RequestAttribute(ATTR_CONNECTION) connection: LDAPConnection,
            @PathVariable("groupCN") groupCN: String,
            @PathVariable("uid") uid: String,
            @RequestParam("all", defaultValue = "false", required = false) listAllGroups: Boolean): String {
        authorizationService.ensureGroupAdministration(currentUserDetails!!, groupCN)
        val user = ldapService.getUserByUid(connection, uid)!!
        val group = with(ldapService.getGroupByCN(connection, groupCN)!!) {
            ldapService.addUserToGroup(connection, user, this)!!
        }
        if (group.hasMember(user.uid, user.dn)) {
            LOG.info("User ${currentUserDetails!!.uid} added user $uid into group $groupCN")
            globalMessageFactory.store(request, globalMessageFactory.createInfo("addUser.success", user.uid, group.cn))

            if (ldapManagementConfiguration.trackedGroups.contains(groupCN)) {
                val recipients = ldapService.getGroupAdmins(connection, group)
                mailService.sendMail(recipients.toList(), GroupChangeMail(currentUserDetails!!, user, group, GroupChangeMail.Action.ADD))
            }
        } else {
            LOG.warn("Adding user $uid into group $groupCN failed")
            globalMessageFactory.store(request, globalMessageFactory.createError("addUser.error", user.uid, group.cn))
        }
        return "redirect:/group/$groupCN?all=$listAllGroups"
    }

    @RequestMapping("/group/{groupCN}/deluser/{uid}", method = [POST])
    fun removeUserFromGroup(
            request: HttpServletRequest,
            @RequestAttribute(ATTR_CONNECTION) connection: LDAPConnection,
            @PathVariable("groupCN") groupCN: String,
            @PathVariable("uid") uid: String,
            @RequestParam("all", defaultValue = "false", required = false) listAllGroups: Boolean): String {
        val details = currentUserDetails
        authorizationService.ensureGroupAdministration(details!!, groupCN)
        val user = ldapService.getUserByUid(connection, uid)!!
        val group = with(ldapService.getGroupByCN(connection, groupCN)!!) {
            ldapService.removeUserFromGroup(connection, user, this)!!
        }

        if (!group.hasMember(user.uid, user.dn)) {
            LOG.info("{} removed user {} from group {}", details.uid, uid, groupCN)
            globalMessageFactory.store(request, globalMessageFactory.createInfo("removeUser.success", user.uid, group.cn))

            if (ldapManagementConfiguration.trackedGroups.contains(groupCN)) {
                val recipients = ldapService.getGroupAdmins(connection, group)
                mailService.sendMail(recipients.toList(), GroupChangeMail(currentUserDetails!!, user, group, GroupChangeMail.Action.REMOVE))
            }
        } else {
            LOG.warn("{} removed user {} from group {}; but it failed", details.uid, uid, groupCN)
            globalMessageFactory.store(request, globalMessageFactory.createError("removeUser.error", user.uid, group.cn))
        }
        return "redirect:/group/$groupCN?all=$listAllGroups"
    }

    @RequestMapping("/group/{groupCN}/authorize", method = [POST])
    fun requestAccess(
            request: HttpServletRequest,
            @RequestAttribute(ATTR_CONNECTION) connection: LDAPConnection,
            @PathVariable("groupCN") groupCN: String,
            @RequestParam("all", defaultValue = "false", required = false) listAllGroups: Boolean): String {
        val details = currentUserDetails!!
        val group = ldapService.getGroupByCN(connection, groupCN) ?: return "redirect:/group?all=$listAllGroups"
        val adminGroup = ldapService.getAdminGroup(connection, group)!!

        if (group.hasMember(details.uid, details.dn)) {
            LOG.info("Current user {} is already a member of group {}", details.username, groupCN)
            globalMessageFactory.store(request, globalMessageFactory.createError("requestAccess.alreadyMember"))
        } else {
            val adminUser = ldapService.getUsersByGroup(connection, adminGroup)
            val success = mailService.sendMail(adminUser.toList(), GroupAccessRequestMail(details, group, adminGroup))
            if (success) {
                LOG.info("${details.uid} requested access to group ${group.cn}. A mail was sent to ${adminUser.size} admins of group ${adminGroup.cn}")
                globalMessageFactory.store(request, globalMessageFactory.createInfo("requestAccess.success"))
            } else {
                LOG.warn("${details.uid} requested access to group ${group.cn}. The request to group ${adminGroup.cn} failed.")
                globalMessageFactory.store(request, globalMessageFactory.createError("requestAccess.failed"))
            }
        }
        return "redirect:/group/$groupCN?all=$listAllGroups"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GroupController::class.java)
    }
}
