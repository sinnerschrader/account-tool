package com.sinnerschrader.s2b.accounttool.presentation.controller;

import com.sinnerschrader.s2b.accounttool.config.WebConstants;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.LogService;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapBusinessService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory;
import com.sinnerschrader.s2b.accounttool.presentation.model.UserForm;
import com.sinnerschrader.s2b.accounttool.presentation.validation.UserFormValidator;
import com.unboundid.ldap.sdk.LDAPConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

/** */
@Controller
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	private static final String FORMNAME_CREATE = "createUserForm";

	private static final String FORMNAME_EDIT = "editUserForm";

	@Autowired
	private LogService logService;

	@Resource(name = "userFormValidator")
	private UserFormValidator userFormValidator;

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private LdapService ldapService;

	@Autowired
	private MailService mailService;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private GlobalMessageFactory globalMessageFactory;

	@Autowired
	private LdapBusinessService ldapBusinessService;

	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public ModelAndView view() {
		authorizationService.ensureUserAdministration(RequestUtils.getCurrentUserDetails());

		ModelAndView mav = new ModelAndView("pages/user/index.html");
		return mav;
	}

	@RequestMapping(value = "/user/maintenance", method = RequestMethod.GET)
	public ModelAndView maintenance() {
		authorizationService.ensureUserAdministration(RequestUtils.getCurrentUserDetails());

		ModelAndView mav = new ModelAndView("pages/user/maintenance.html");
		mav.addObject("leavingUsers", ldapBusinessService.getLeavingUsers());
		mav.addObject("unmaintainedUsers", ldapBusinessService.getUnmaintainedExternals());
		mav.addObject("unmaintainedMailUsers", ldapBusinessService.getUnmaintainedMailUsers());
		return mav;
	}

	@RequestMapping(value = "/user/search", method = RequestMethod.GET)
	public ModelAndView searchUser(
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@RequestParam(name = "searchTerm") String searchTerm) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureUserAdministration(details);

		List<User> users = new LinkedList<>();
		if (StringUtils.isNotBlank(searchTerm)) {
			users.addAll(ldapService.findUserBySearchTerm(connection, searchTerm));
		}

		ModelAndView mav = new ModelAndView("pages/user/userSearch.html");
		mav.addObject("company", ldapConfiguration.getCompaniesAsMap());
		mav.addObject("users", users);
		return mav;
	}

	@RequestMapping(value = "/user/edit/{userId}", method = RequestMethod.GET)
	public ModelAndView edit(
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "userId") String userId,
			Model model) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureUserAdministration(details);

		User user = ldapService.getUserByUid(connection, userId);
		if (StringUtils.isBlank(userId) || user == null) {
			return new ModelAndView("redirect:/user");
		}
		if (!model.containsAttribute(FORMNAME_EDIT)) {
			model.addAttribute(FORMNAME_EDIT, new UserForm(user));
		}
		ModelAndView mav = new ModelAndView();
		mav.addAllObjects(model.asMap());
		mav.addObject("user", user);
		mav.addObject("companies", ldapConfiguration.getCompaniesAsMap());
		mav.addObject("types", ldapService.getEmployeeType(connection));
		mav.addObject("departments", ldapService.getDepartments(connection));
		mav.addObject("locations", ldapService.getLocations(connection));
		mav.setViewName("pages/user/edit.html");
		return mav;
	}

	@RequestMapping(value = "/user/edit/{userId}", method = RequestMethod.POST)
	public String performEdit(
			HttpServletRequest request,
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@PathVariable(name = "userId") String userId,
			@ModelAttribute(FORMNAME_EDIT) @Valid UserForm userForm,
			RedirectAttributes attr,
			BindingResult bindingResult) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureUserAdministration(details);

		// half security check, if readonly field was manipulated
		if (!StringUtils.equals(userForm.getUid(), userId)) {
			throw new IllegalArgumentException("Form submit was modified");
		}

		userFormValidator.validate(userForm, bindingResult);
		if (bindingResult.hasErrors()) {
			attr.addFlashAttribute(BindingResult.class.getName() + "." + FORMNAME_EDIT, bindingResult);
			attr.addFlashAttribute(FORMNAME_EDIT, userForm);
			return "redirect:/user/edit/" + userId;
		}
		try {
			if (userForm.isChangeUser()) {
				User user =
						ldapService.update(connection, userForm.createUserEntityFromForm(ldapConfiguration));
				globalMessageFactory.store(
						request, globalMessageFactory.createInfo("user.edit.success", user.getUid()));
				log.info("{} updated the account of user {}", details.getUid(), user.getUid());
			}
			if (userForm.isResetpassword() || userForm.isActivateUser() || userForm.isDeactivateUser()) {
				boolean hidePassword = false;
				User user = ldapService.getUserByUid(connection, userId);
				String password = ldapService.resetPassword(connection, user);
				// update current user details for correct bind on next request
				if (StringUtils.equals(details.getUid(), user.getUid())) {
					details.setPassword(password);
				}
				if (userForm.isResetpassword()) {
					log.info("{} reseted the password of user {}", details.getUid(), user.getUid());
					logService.event(
							"logging.logstash.event.user.password-reset",
							"success",
							details.getUid(),
							user.getUid());
				}
				String message = hidePassword ? "user.passwordReset.simple" : "user.passwordReset.full";
				if (userForm.isActivateUser()) {
					message = "user.activated";
					ldapService.activate(connection, user);
					ldapBusinessService.addDefaultGroups(user);
					log.info("{} activated the user {} right now", details.getUid(), user.getUid());
					logService.event(
							"logging.logstash.event.user.activate", "success", details.getUid(), user.getUid());
				}
				if (userForm.isDeactivateUser()) {
					message = "user.deactivated";
					ldapService.deactivate(connection, user);
					ldapBusinessService.delDefaulGroups(user);
					log.info("{} deactivated the user {} right now", details.getUid(), user.getUid());
					logService.event(
							"logging.logstash.event.user.deactivate", "success", details.getUid(), user.getUid());
				}
				globalMessageFactory.store(
						request, globalMessageFactory.createInfo(message, user.getUid(), password));
				return "redirect:/user/edit/" + userId;
			}
		} catch (BusinessException be) {
			bindingResult.reject(be.getCode(), be.getArgs(), "Could not update user");
			attr.addFlashAttribute(BindingResult.class.getName() + "." + FORMNAME_EDIT, bindingResult);
			attr.addFlashAttribute(FORMNAME_EDIT, userForm);
			return "redirect:/user/edit/" + userId;
		}
		return "redirect:/user";
	}

	@RequestMapping(value = "/user/create", method = RequestMethod.GET)
	public ModelAndView create(
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			Model model) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureUserAdministration(details);

		if (!model.containsAttribute(FORMNAME_CREATE)) {
			model.addAttribute(FORMNAME_CREATE, new UserForm(details));
		}
		ModelAndView mav = new ModelAndView();
		mav.addAllObjects(model.asMap());
		mav.addObject("primaryDomain", userFormValidator.getPrimaryDomain());
		mav.addObject("secondaryDomain", userFormValidator.getSecondaryDomain());
		mav.addObject("companies", ldapConfiguration.getCompaniesAsMap());
		mav.addObject("types", ldapService.getEmployeeType(connection));
		mav.addObject("departments", ldapService.getDepartments(connection));
		mav.addObject("locations", ldapService.getLocations(connection));
		mav.setViewName("pages/user/create.html");
		return mav;
	}

	@RequestMapping(value = "/user/create", method = RequestMethod.POST)
	public String performCreate(
			HttpServletRequest request,
			@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
			@ModelAttribute(FORMNAME_CREATE) @Valid UserForm userForm,
			BindingResult bindingResult,
			RedirectAttributes attr) {
		LdapUserDetails currentUser = RequestUtils.getCurrentUserDetails();
		authorizationService.ensureUserAdministration(currentUser);

		userFormValidator.validate(userForm, bindingResult);
		if (bindingResult.hasErrors()) {
			attr.addFlashAttribute(BindingResult.class.getName() + "." + FORMNAME_CREATE, bindingResult);
			attr.addFlashAttribute(FORMNAME_CREATE, userForm);
			return "redirect:/user/create";
		}
		try {
			User newUser =
					ldapService.insert(connection, userForm.createUserEntityFromForm(ldapConfiguration));
			String password = ldapService.resetPassword(connection, newUser);
			ldapBusinessService.addDefaultGroups(newUser);
			globalMessageFactory.store(
					request,
					globalMessageFactory.createInfo("user.create.success", newUser.getUid(), password));
			log.info("{} created a new account with uid {}", currentUser.getUid(), newUser.getUid());
			logService.event(
					"logging.logstash.event.user.create", "success", currentUser.getUid(), newUser.getUid());
		} catch (BusinessException be) {
			bindingResult.reject(be.getCode(), be.getArgs(), "Could not create user");
			attr.addFlashAttribute(BindingResult.class.getName() + "." + FORMNAME_CREATE, bindingResult);
			attr.addFlashAttribute(FORMNAME_CREATE, userForm);
			return "redirect:/user/create";
		}
		return "redirect:/user";
	}
}
