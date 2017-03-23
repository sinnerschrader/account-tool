package com.sinnerschrader.s2b.accounttool.presentation.controller;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Controller to handle errors
 */
@Controller
@ControllerAdvice
public class ErrorController {

	private static final Logger log = LoggerFactory.getLogger(ErrorController.class);

	@ExceptionHandler({AccessDeniedException.class, HttpRequestMethodNotSupportedException.class})
	public ModelAndView handleAccessDenied(HttpServletRequest request, Exception ex) {
		String calledUrl = request.getRequestURL().toString();
		log.warn("Request: " + calledUrl + " raised " + ex.getClass());
		ModelAndView mav = new ModelAndView("pages/error404.html");
		mav.addObject("url", calledUrl);
		return mav;
	}

	@ExceptionHandler({Exception.class, RuntimeException.class})
	public ModelAndView handleError(HttpServletRequest request, Exception ex) {
		String calledUrl = request.getRequestURL().toString();
		log.error("Request: {} raised {}", calledUrl, ex.getClass());
		if (log.isDebugEnabled()) {
			log.error("Request: {} raised error: ", calledUrl, ex);
		}

		StringWriter fullStackTrace = new StringWriter();
		ex.printStackTrace(new PrintWriter(fullStackTrace));

		ModelAndView mav = new ModelAndView("pages/error500.html");
		mav.addObject("exception", ex);
		mav.addObject("fullStackTrace", fullStackTrace);
		mav.addObject("url", request.getRequestURL());
		return mav;
	}

	@RequestMapping("/403")
	public String accessDenied(HttpServletRequest request) {
		return "pages/error403.html";
	}

	@RequestMapping("/500")
	public String internalError(HttpServletRequest request) {
		return "pages/error500.html";
	}

	@RequestMapping("/404")
	public String notFound(HttpServletRequest request) {
		return "pages/error404.html";
	}
}
