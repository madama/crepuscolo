package net.etalia.crepuscolo.auth;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class RefreshAuthTokenFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
		AuthData auth = AuthFilter.getAuthData();
		if (auth.getCurrentToken() != null) {
			if (response instanceof HttpServletResponse) {
				String newToken = AuthData.produce(auth.getUserId(), auth.getUserPassword(), auth.getSystemId());
				((HttpServletResponse) response).setHeader("X-Authorization", newToken);
			}
		}
	}

	@Override
	public void destroy() {
	}

}
