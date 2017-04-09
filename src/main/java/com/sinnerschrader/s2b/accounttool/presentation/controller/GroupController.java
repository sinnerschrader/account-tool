package com.sinnerschrader.s2b.accounttool.presentation.controller;

import com.sinnerschrader.s2b.accounttool.config.WebConstants;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapGroupPrefixes;
import com.sinnerschrader.s2b.accounttool.logic.LogService;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory;
import com.unboundid.ldap.sdk.LDAPConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

/** */
@Controller
public class GroupController {

	private static final Logger log = LoggerFactory.getLogger(GroupController.class);

	@Autowired
	private LogService logService;

	@Autowired
	private LdapService ldapService;

	@Autowired
	private MailService mailService;

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private GlobalMessageFactory globalMessageFactory;

	@RequestMapping(value = "/group", method = RequestMethod.GET)
	public ModelAndView listGroups(
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION, required = true)
					LDAPConnection connection,
			@RequestParam(name = "all", defaultValue = "false", required = false) boolean listAllGroups) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		if (details == null) {
			throw new IllegalStateException("Invalid access on groups");
		}
		List<Group> groups =
				listAllGroups
						? ldapService.getGroups(connection)
						: ldapService.getGroupsByUser(connection, details.getUid(), details.getDn());

		ModelAndView mav = new ModelAndView("pages/group/index.html");
		mav.addObject("groups", groups);
		mav.addObject("showAllGroups", listAllGroups);
		return mav;
	}

	@RequestMapping(value = "/group/{groupCN}", method = RequestMethod.GET)
	public ModelAndView viewGroup(
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "groupCN") String groupCN,
			@RequestParam(name = "messageKey", defaultValue = "", required = false) String messageKey,
			@RequestParam(name = "all", defaultValue = "false", required = false) boolean listAllGroups) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		if (details == null) {
			throw new IllegalStateException("Invalid access on groups");
		}
		List<Group> groups =
				(listAllGroups)
						? ldapService.getGroups(connection)
						: ldapService.getGroupsByUser(connection, details.getUid(), details.getDn());
		Group selectedGroup = ldapService.getGroupByCN(connection, groupCN);
		if (selectedGroup == null) {
			return new ModelAndView("redirect:/group");
		}
		List<User> users = ldapService.getUsersByGroup(connection, selectedGroup);

		ModelAndView mav = new ModelAndView("pages/group/index.html");
		mav.addObject("company", ldapConfiguration.getCompaniesAsMap());
		mav.addObject("messageKey", messageKey);
		mav.addObject("showAllGroups", listAllGroups);
		mav.addObject("groups", groups);
		mav.addObject("selectedGroup", selectedGroup);
		mav.addObject("usersByGroup", users);
		return mav;
	}

	@RequestMapping(value = "/group/{groupCN}/search", method = RequestMethod.GET)
	public ModelAndView searchUserForGroup(
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "groupCN") String groupCN,
			@RequestParam(name = "searchTerm") String searchTerm,
			@RequestParam(name = "all", defaultValue = "false", required = false) boolean listAllGroups) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureGroupAdministration(details, groupCN);
		Group group = ldapService.getGroupByCN(connection, groupCN);
		if (group == null) {
			return new ModelAndView("redirect:/group/" + groupCN);
		}
		List<User> users = new LinkedList<>();
		if (StringUtils.isNotBlank(searchTerm)) {
			List<User> ldapUsers = ldapService.findUserBySearchTerm(connection, searchTerm);
			for (User user : ldapUsers) {
				if (!group.hasMember(user)) {
					users.add(user);
				}
			}
		}

		ModelAndView mav = new ModelAndView("pages/group/userSearch.html");
		mav.addObject("company", ldapConfiguration.getCompaniesAsMap());
		mav.addObject("group", group);
		mav.addObject("users", users);
		mav.addObject("showAllGroups", listAllGroups);
		return mav;
	}

	@RequestMapping(value = "/group/{groupCN}/adduser/{uid}", method = RequestMethod.POST)
	public String addUserToGroup(
			HttpServletRequest request,
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "groupCN") String groupCN,
			@PathVariable(name = "uid") String uid,
			@RequestParam(name = "all", defaultValue = "false", required = false) boolean listAllGroups) {
		final String eventKey = "logging.logstash.event.group.user.add";
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureGroupAdministration(details, groupCN);
		User user = ldapService.getUserByUid(connection, uid);
		Group group = ldapService.getGroupByCN(connection, groupCN);
		group = ldapService.addUserToGroup(connection, user, group);
		if (group.hasMember(user)) {
			log.info("User {} added user {} into group {}", details.getUid(), uid, groupCN);
			logService.event(eventKey, "success", details.getUid(), uid, groupCN);
			globalMessageFactory.store(
					request,
					globalMessageFactory.createInfo("addUser.success", user.getUid(), group.getCn()));
		} else {
			log.warn("Adding user {} into group {} failed", uid, groupCN);
			logService.event(eventKey, "success", details.getUid(), uid, groupCN);
			globalMessageFactory.store(
					request, globalMessageFactory.createError("addUser.error", user.getUid(), group.getCn()));
		}
		return "redirect:/group/" + groupCN + (listAllGroups ? "?all=true" : "");
	}

	@RequestMapping(value = "/group/{groupCN}/deluser/{uid}", method = RequestMethod.POST)
	public String removeUserFromGroup(
			HttpServletRequest request,
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "groupCN") String groupCN,
			@PathVariable(name = "uid") String uid,
			@RequestParam(name = "all", defaultValue = "false", required = false) boolean listAllGroups) {
		final String eventKey = "logging.logstash.event.group.user.del";
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureGroupAdministration(details, groupCN);
		User user = ldapService.getUserByUid(connection, uid);
		Group group = ldapService.getGroupByCN(connection, groupCN);
		group = ldapService.removeUserFromGroup(connection, user, group);
		if (!group.hasMember(user)) {
			log.info("{} removed user {} from group {}", details.getUid(), uid, groupCN);
			logService.event(eventKey, "success", details.getUid(), uid, groupCN);
			globalMessageFactory.store(
					request,
					globalMessageFactory.createInfo("removeUser.success", user.getUid(), group.getCn()));
		} else {
			log.warn("{} removed user {} from group {}; but it failed", details.getUid(), uid, groupCN);
			logService.event(eventKey, "success", details.getUid(), uid, groupCN);
			globalMessageFactory.store(
					request,
					globalMessageFactory.createError("removeUser.error", user.getUid(), group.getCn()));
		}
		return "redirect:/group/" + groupCN + (listAllGroups ? "?all=true" : "");
	}

	@RequestMapping(value = "/group/{groupCN}/authorize", method = RequestMethod.POST)
	public String requestAccess(
			HttpServletRequest request,
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "groupCN") String groupCN,
			@RequestParam(name = "all", defaultValue = "false", required = false) boolean listAllGroups) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		Group group = ldapService.getGroupByCN(connection, groupCN);
		Group adminGroup = group;

		if (group == null) {
			return "redirect:/group" + (listAllGroups ? "?all=true" : "");
		}

		if (group.hasMember(details.getUid()) || group.hasMember(details.getDn())) {
			log.info("Current user {} is already a member of group {}", details.getUsername(), groupCN);
			globalMessageFactory.store(
					request, globalMessageFactory.createError("requestAccess.alreadyMember"));
		} else {
			if (!adminGroup.isAdminGroup()) {
				LdapGroupPrefixes gp = ldapConfiguration.getGroupPrefixes();
				String adminGroupCN = StringUtils.replace(groupCN, gp.getTeam(), gp.getAdmin());
				adminGroup = ldapService.getGroupByCN(connection, adminGroupCN);
				if (adminGroup == null || !adminGroup.isAdminGroup()) {
					adminGroup = ldapService.getGroupByCN(connection, "ldap-admins");
				}
			}
			List<User> adminUser = ldapService.getUsersByGroup(connection, adminGroup);
			boolean success =
					mailService.sendMailForRequestAccessToGroup(details, adminUser, adminGroup, group);
			if (success) {
				log.info(
						"{} requested access to group {}. A mail was sent to {} admins of group {}",
						details.getUid(),
						group.getCn(),
						adminUser.size(),
						adminGroup.getCn());
				globalMessageFactory.store(
						request, globalMessageFactory.createInfo("requestAccess.success"));
			} else {
				log.warn(
						"{} requested access to group {}. The request to group {} failed.",
						details.getUid(),
						group.getCn(),
						adminGroup.getCn());
				globalMessageFactory.store(
						request, globalMessageFactory.createError("requestAccess.failed"));
			}
		}
		return "redirect:/group/" + groupCN + (listAllGroups ? "?all=true" : "");
	}
}
