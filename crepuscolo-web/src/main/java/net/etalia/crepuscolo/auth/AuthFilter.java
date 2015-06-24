package net.etalia.crepuscolo.auth;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.etalia.crepuscolo.check.CheckAspect;

public class AuthFilter implements Filter {

	private static final ThreadLocal<AuthData> authData = new ThreadLocal<AuthData>();
	private Set<Integer> validPorts = new HashSet<Integer>();
	private String guestAuth;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		guestAuth = filterConfig.getInitParameter("guestAuth");
		if (guestAuth == null) guestAuth = "__GUEST__";
		guestAuth = "Etalia " + guestAuth;
		
		String ports = filterConfig.getInitParameter("securePorts");
		if (ports != null) {
			String[] split = ports.split(",");
			for (String ps : split) {
				validPorts.add(Integer.parseInt(ps));
			}
		}
	}

	public static AuthData getAuthData() {
		AuthData ret = authData.get();
		if (ret == null) ret = new AuthData(null, false, false);
		return ret;
	}

	public static AuthData newAuthData() {
		AuthData ret = getAuthData();
		authData.set(ret);
		return ret;
	}

	public static void clearAuthData() {
		authData.remove();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// Cast, this filter works only on HTTP (as if servlets are really used for anything else)
		HttpServletRequest hrequest = (HttpServletRequest) request;
		
		try {
			// Get the auth header
			String auth = hrequest.getHeader("Authorization");
			// If it exists and starts with "Etalia " then it's our 
			if (auth != null && !auth.equals(guestAuth) && auth.startsWith("Crepuscolo ")) {
				// Save the token
				
				
				// Remove "Etalia "
				auth = auth.substring("Etalia ".length());

				// Check ports
				int localPort = request.getLocalPort();
				
				try {
					// Create and store auth data
					authData.set(new AuthData(auth, validPorts.contains(localPort), request.isSecure()));
				} catch (Exception e) {
					((HttpServletResponse)response).sendError(301, "Invalid header");
				}
			}
			
			// Proceed in the chain
			chain.doFilter(request, response);
			
		} finally {
			// Avoid nasty to debug memory leaks
			authData.remove();
			CheckAspect.aspectOf().clear();
		}
	}

	@Override
	public void destroy() {
		
	}

}
