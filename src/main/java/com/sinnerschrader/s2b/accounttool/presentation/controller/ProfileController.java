package com.sinnerschrader.s2b.accounttool.presentation.controller;

import com.sinnerschrader.s2b.accounttool.config.WebConstants;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.LogService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import com.sinnerschrader.s2b.accounttool.presentation.model.ChangeProfile;
import com.sinnerschrader.s2b.accounttool.presentation.validation.ChangeProfileFormValidator;
import com.unboundid.ldap.sdk.LDAPConnection;

import javax.annotation.Resource;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 *
 */
@Controller
public class ProfileController
{

	private final static Logger log = LoggerFactory.getLogger(ProfileController.class);

	private final static String FORMNAME = "changeProfileForm";

	@Autowired
	private LogService logService;

	@Autowired
	private LdapService ldapService;

	@Resource(name = "changeProfileFormValidator")
	private ChangeProfileFormValidator changeProfileFormValidator;

	@RequestMapping(path = "/profile", method = RequestMethod.GET)
	public ModelAndView profile(HttpServletRequest request, Model model)
	{
		LDAPConnection connection = RequestUtils.getLdapConnection(request);
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		ModelAndView mav = new ModelAndView("pages/profile/index.html");
		if (details != null)
		{
			User user = ldapService.getUserByUid(connection, details.getUsername());
			mav.addAllObjects(model.asMap());
			mav.addObject("user", user);
			mav.addObject("groups", ldapService.getGroupsByUser(connection, details.getUid(), details.getDn()));
			if (!model.containsAttribute(FORMNAME))
			{
				model.addAttribute(FORMNAME, new ChangeProfile(user));
			}
		}
		return mav;
	}

	@RequestMapping(value = "/profile/change", method = RequestMethod.POST)
	public String changeCurrentUserAttributes(
		HttpServletRequest request,
		@RequestAttribute(name = WebConstants.ATTR_CONNECTION) LDAPConnection connection,
		@ModelAttribute(name = FORMNAME) ChangeProfile form,
		RedirectAttributes attr,
		BindingResult bindingResult)
	{
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		changeProfileFormValidator.validate(form, bindingResult);
		if (bindingResult.hasErrors())
		{
			attr.addFlashAttribute(BindingResult.class.getName() + "." + FORMNAME, bindingResult);
			attr.addFlashAttribute(FORMNAME, form);
		}
		else
		{
			User ldapUser = ldapService.getUserByUid(connection, details.getUid());
			User updatedUser = form.createUserEntityFromForm(ldapUser);
			if (form.isPasswordChange())
			{
				boolean res = ldapService.changePassword(connection, details, form.getPassword());
				String state = res ? "sucess" : "failure";
				log.info("{} changed his/her password", details.getUid());
				logService.event("logging.logstash.event.password-change", state, details.getUid());
			}
			else
			{
				try
				{
					ldapService.update(connection, details, updatedUser);
					log.info("{} updated his/her account informations", details.getUid());
				}
				catch (BusinessException be)
				{
					bindingResult.reject(be.getCode(), be.getArgs(), "Could not create user");
					attr.addFlashAttribute(BindingResult.class.getName() + "." + FORMNAME, bindingResult);
					attr.addFlashAttribute(FORMNAME, form);
				}
			}
		}
		return "redirect:/profile";
	}

}
