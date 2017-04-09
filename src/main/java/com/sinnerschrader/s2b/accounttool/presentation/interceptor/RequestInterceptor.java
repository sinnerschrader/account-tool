package com.sinnerschrader.s2b.accounttool.presentation.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple Request Interceptor which provides some global Objects / Informations
 */
public class RequestInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(RequestInterceptor.class);

	private Environment environment;

	public RequestInterceptor(Environment environment) {
		this.environment = environment;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		return true;
	}

	@Override
	public void postHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler,
			ModelAndView modelAndView)
			throws Exception {
		log.trace("{} {}", request.getMethod(), request.getRequestURI());
		if (StringUtils.equals(request.getMethod(), "GET")
				&& modelAndView != null
				&& !StringUtils.startsWithAny(modelAndView.getViewName(), "redirect:")
				&& environment != null) {
			modelAndView.addObject("env", environment);
			modelAndView.addObject("version", environment.getProperty("app.version"));
			modelAndView.addObject("rev", environment.getProperty("app.revision"));
			modelAndView.addObject("time", environment.getProperty("app.build-time"));
		}
	}

	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}
}
