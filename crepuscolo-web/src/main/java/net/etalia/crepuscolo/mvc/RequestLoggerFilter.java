package net.etalia.crepuscolo.mvc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestLoggerFilter implements Filter {

	protected Log log = LogFactory.getLog(RequestLoggerFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		long start = System.currentTimeMillis();
		chain.doFilter(request, response);
		long end = System.currentTimeMillis();
		long time = end - start;
		String url = null;
		String queryString = null;
		String method = null;
		if (request instanceof HttpServletRequest) {
			url = ((HttpServletRequest)request).getContextPath();
			url += ((HttpServletRequest)request).getServletPath();
			queryString = ((HttpServletRequest)request).getQueryString();
			method = ((HttpServletRequest)request).getMethod();
		}
		int code = -1;
		if (response instanceof HttpServletResponse) {
			code = ((HttpServletResponse) response).getStatus();
		}
		log.info("TIME: " + time + " - CODE: " + code + " - METHOD: " + method + " - URL: " + url + " - PARAMS: " + queryString);
	}

	@Override
	public void destroy() {
	}

}
