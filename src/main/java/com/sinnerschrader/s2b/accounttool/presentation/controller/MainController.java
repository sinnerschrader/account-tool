package com.sinnerschrader.s2b.accounttool.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sinnerschrader.s2b.accounttool.config.WebConstants;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.LogService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import com.sinnerschrader.s2b.accounttool.logic.component.licences.LicenseSummary;
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Controller
public class MainController {

	private static final Logger log = LoggerFactory.getLogger(MainController.class);

	@Autowired
	private Environment environment;

	@Autowired
	private LicenseSummary licenseSummary;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private LdapService ldapService;

	@Autowired
	private LogService logService;

	@RequestMapping(path = "/")
	public String root() {
		return "redirect:/profile";
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView login(
			HttpServletRequest request,
			@CookieValue(name = WebConstants.COMPANY_COOKIE_NAME, defaultValue = "") String company,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout) {
		ModelAndView model = new ModelAndView("pages/login.html");
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		if (details != null) {
			// redirect on login page, if the user is already logged in.
			return new ModelAndView("redirect:/profile");
		}
		if (error != null) {
			model.addObject("error", getErrorCode(request));
		}
		if (logout != null) {
			model.addObject("msg", "logout.success");
		}
		model.addObject("selectedCompany", company);
		model.addObject("companies", ldapConfiguration.getCompaniesAsMap());
		return model;
	}

	//customize the error message
	private String getErrorCode(HttpServletRequest request) {
		final String key = "SPRING_SECURITY_LAST_EXCEPTION";
		Exception exception = (Exception) request.getSession().getAttribute(key);

		String error;
		if (exception instanceof BadCredentialsException) {
			error = "login.invalid.credentials";
		} else if (exception instanceof DisabledException) {
			error = "login.account.disabled";
		} else {
			error = "login.general.error";
		}

		return error;
	}

	@RequestMapping(path = "/logout", method = RequestMethod.GET)
	public String logout(HttpServletRequest request) {
		LdapUserDetails details = RequestUtils.getCurrentUserDetails();
		if (details != null) {
			log.debug("{} has been successfully logged off", details.getUid());
			logService.event("logging.logstash.event.logout", "success", details.getUid());
		}
		request.getSession().invalidate();
		return "redirect:/login?logout";
	}

	@RequestMapping(path = "/version")
	public ModelAndView version() {
		Set<String> activeProfiles = new LinkedHashSet<>();
		activeProfiles.addAll(Arrays.asList(environment.getDefaultProfiles()));
		activeProfiles.addAll(Arrays.asList(environment.getActiveProfiles()));

		Date contextStartupDate = new Date(applicationContext.getStartupDate());
		ModelAndView model = new ModelAndView("pages/version.html");
		model.addObject("applicationContextStartupDate", contextStartupDate);
		model.addObject(
				"applicationContextRunningSince", new PrettyTime().formatUnrounded(contextStartupDate));
		model.addObject("activeProfiles", activeProfiles);
		return model;
	}

	@RequestMapping(path = "/license")
	public ModelAndView licences() {
		return new ModelAndView("pages/license.html")
				.addObject("licenseSummary", licenseSummary)
				.addObject("env", environment);
	}

	@RequestMapping(path = "/csp-report", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> cspReport(@RequestBody(required = false) String cspReportString) {
		HttpStatus status;
		if (cspReportString != null) {
			status = HttpStatus.OK;
			try {
				ObjectMapper om = new ObjectMapper();
				JsonNode cspReportRoot = om.readTree(cspReportString);

				om.enable(SerializationFeature.INDENT_OUTPUT);
				StringWriter writer = new StringWriter();
				om.writeValue(writer, cspReportRoot);
				log.warn("CSP-Report: \n{}", writer.toString());
			} catch (Exception e) {
				// ignore exception and print request body plain
				log.warn("CSP-Report: {}", cspReportString);
			}
		} else {
			status = HttpStatus.NOT_FOUND;
		}
		return new ResponseEntity<>(status);
	}
}
