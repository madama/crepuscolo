package net.etalia.crepuscolo.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class AuthCheckServletRequest extends HttpServletRequestWrapper {

	private String method;
	private String path;

	private Map<String,String> overriddenHeaders = new HashMap<String, String>();

	public AuthCheckServletRequest(HttpServletRequest request, String method, String path) {
		super(request);
		this.method = method;
		this.path = path;
	}

	@Override
	public String getParameter(String name) {
		String param = super.getParameter(name);
		if (param == null) return "0";
		return param;
	}

	@Override
	public String getHeader(String name) {
		String header = overriddenHeaders.get(name); 
		if (header != null) return header;
		header = super.getHeader(name);
		if (header != null) return header;
		return "0";
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		String header = overriddenHeaders.get(name);
		if (header != null) return Collections.enumeration(Arrays.asList(header));
		return super.getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		ArrayList<String> ret = new ArrayList<String>(Collections.list(super.getHeaderNames()));
		ret.addAll(overriddenHeaders.keySet());
		return Collections.enumeration(ret);
	}

	public void overrideHeader(String name, String value) {
		overriddenHeaders.put(name, value);
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getPathInfo() {
		return path;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStream() {
			private String CONTENT = "{}";
			private int step = -1;
			@Override
			public int read() throws IOException {
				step++;
				if (step >= CONTENT.length()) return -1;
				return CONTENT.charAt(step);
			}
			@Override
			public boolean isFinished() {
				// TODO Auto-generated method stub
				return false;
			}
			@Override
			public boolean isReady() {
				// TODO Auto-generated method stub
				return false;
			}
			@Override
			public void setReadListener(ReadListener readListener) {
				// TODO Auto-generated method stub
			}
		};
	}

}
