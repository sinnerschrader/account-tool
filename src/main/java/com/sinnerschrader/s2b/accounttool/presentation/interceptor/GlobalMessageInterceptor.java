package com.sinnerschrader.s2b.accounttool.presentation.interceptor;

import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessage;
import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Interceptor for displaying messages from application to the user. The messages have a validity to
 * the next request (next GET request).
 */
public class GlobalMessageInterceptor implements HandlerInterceptor {

	private static final String ATTRIBUTE_NAME = "globalMessages";

	@Autowired
	private GlobalMessageFactory globalMessageFactory;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (StringUtils.equalsIgnoreCase(request.getMethod(), "get")) {
			List<GlobalMessage> globalMessages = globalMessageFactory.pop(request);
			request.setAttribute(ATTRIBUTE_NAME, globalMessages);
		}
		return true;
	}

	@Override
	public void postHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler,
			ModelAndView modelAndView)
			throws Exception {
	}

	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}
}
