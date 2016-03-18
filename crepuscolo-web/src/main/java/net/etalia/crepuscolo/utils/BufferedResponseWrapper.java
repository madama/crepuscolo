package net.etalia.crepuscolo.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.cookie.DateUtils;

public class BufferedResponseWrapper implements HttpServletResponse {

	private HttpServletResponse original;

	private String characterEncoding;
	private String contentType;

	private Integer contentLength;

	private Integer bufferSize;

	private Locale locale;

	private Set<Cookie> cookies;

	private Map<String,List<String>> headers = new HashMap<>();

	private Integer status;

	private String statusMsg;

	private String redirect;

	private ByteArrayOutputStream output;

	private boolean considerCommitted = false;

	public BufferedResponseWrapper(HttpServletResponse original) {
		this.original = original;
	}

	public void commit() throws IOException {
		if (original.isCommitted()) throw new IllegalStateException();
		if (characterEncoding != null) {
			original.setCharacterEncoding(characterEncoding);
		}
		if (contentType != null) {
			original.setContentType(contentType);
		}
		if (contentLength != null) {
			original.setContentLength(contentLength);
		}
		if (bufferSize != null) {
			original.setBufferSize(bufferSize);
		}
		if (locale != null) {
			original.setLocale(locale);
		}
		if (headers != null) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				boolean first = true;
				for (String val : entry.getValue()) {
					if (first) {
						original.setHeader(entry.getKey(), val);
					} else {
						original.addHeader(entry.getKey(), val);
					}
					first = false;
				}
			}
		}
		if (cookies != null) {
			for (Cookie c : this.cookies) {
				original.addCookie(c);
			}
		}
		
		if (redirect != null) {
			original.sendRedirect(redirect);
		} else if (this.statusMsg != null) {
			if (this.considerCommitted) {
				original.sendError(status, statusMsg);
			} else {
				original.setStatus(status, statusMsg);
			}
		} else if (this.status != null) {
			if (this.considerCommitted) {
				original.sendError(status);
			} else {
				original.setStatus(status);
			}
		}

		if (output != null) {
			byte[] data = output.toByteArray();
			if (data.length > 0) {
				original.getOutputStream().write(data);
			}
		}
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (output == null) output = new ByteArrayOutputStream();
		return new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				output.write(b);
			}
			@Override
			public boolean isReady() {
				// TODO Auto-generated method stub
				return false;
			}
			@Override
			public void setWriteListener(WriteListener writeListener) {
				// TODO Auto-generated method stub
			}
		};
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(getOutputStream());
	}

	@Override
	public void flushBuffer() throws IOException {
		if (original.isCommitted()) throw new IllegalStateException();
	}

	@Override
	public void resetBuffer() {
		if (original.isCommitted()) throw new IllegalStateException();
		if (output != null) output.reset();
	}

	@Override
	public void reset() {
		this.characterEncoding = null;
		this.bufferSize = null;
		this.contentLength = null;
		this.contentType = null;
		this.cookies = null;
		this.headers = null;
		this.locale = null;
		this.output = null;
		this.redirect = null;
		this.status = null;
		this.statusMsg = null;
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	
	@Override
	public void setCharacterEncoding(String charset) {
		this.characterEncoding = charset;
	}

	@Override
	public void setContentLength(int len) {
		this.contentLength = len;
	}

	@Override
	public void setContentType(String type) {
		this.contentType = type;
	}

	@Override
	public void setBufferSize(int size) {
		this.bufferSize = size;
	}

	@Override
	public int getBufferSize() {
		return this.bufferSize == null ? 0 : this.bufferSize;
	}

	@Override
	public boolean isCommitted() {
		return original.isCommitted() || considerCommitted;
	}

	@Override
	public void setLocale(Locale loc) {
		this.locale = loc;
	}

	@Override
	public Locale getLocale() {
		return this.locale;
	}

	@Override
	public void addCookie(Cookie cookie) {
		if (this.cookies == null) this.cookies = new HashSet<>();
		this.cookies.add(cookie);
	}
	
	@Override
	public String encodeURL(String url) {
		return original.encodeURL(url);
	}

	@Override
	public String encodeRedirectURL(String url) {
		return original.encodeRedirectURL(url);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String encodeUrl(String url) {
		return original.encodeUrl(url);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String encodeRedirectUrl(String url) {
		return original.encodeRedirectUrl(url);
	}


	@Override
	public boolean containsHeader(String name) {
		if (this.headers == null) return false;
		return this.headers.containsKey(name);
	}	
	
	@Override
	public void sendError(int sc, String msg) throws IOException {
		if (this.isCommitted()) throw new IllegalStateException();
		this.considerCommitted = true;
		this.status = sc;
		this.statusMsg = msg;
	}

	@Override
	public void sendError(int sc) throws IOException {
		this.sendError(sc, null);
	}

	@Override
	public void setStatus(int sc) {
		this.setStatus(sc, null);
	}

	@Override
	public void setStatus(int sc, String sm) {
		this.status = sc;
		this.statusMsg = sm;
	}

	@Override
	public int getStatus() {
		return this.status == null ? 200 : this.status;
	}

	
	@Override
	public void sendRedirect(String location) throws IOException {
		if (this.isCommitted()) throw new IllegalStateException();
		this.considerCommitted = true;
		this.redirect = location;
	}

	@Override
	public void setDateHeader(String name, long date) {
		this.setHeader(name, DateUtils.formatDate(new Date(date)));
	}

	@Override
	public void addDateHeader(String name, long date) {
		this.addHeader(name, DateUtils.formatDate(new Date(date)));
	}

	@Override
	public void setIntHeader(String name, int value) {
		this.setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(String name, int value) {
		this.addHeader(name, Integer.toString(value));
	}

	@Override
	public String getHeader(String name) {
		if (this.headers == null) return null;
		List<String> list = this.headers.get(name);
		if (list == null || list.isEmpty()) return null;
		return list.get(0);
	}

	@Override
	public void setHeader(String name, String value) {
		if (this.headers == null) this.headers = new HashMap<String, List<String>>();
		this.headers.put(name, new ArrayList<>(Arrays.asList(value)));
	}

	@Override
	public void addHeader(String name, String value) {
		if (this.headers == null) this.headers = new HashMap<String, List<String>>();
		List<String> list = this.headers.get(name);
		if (list == null) {
			list = new ArrayList<>();
			this.headers.put(name, list);
		}
		list.add(value);
	}

	
	@Override
	public Collection<String> getHeaders(String name) {
		for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
			if (entry.getKey().equals(name)) return entry.getValue();
		}
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getHeaderNames() {
		return this.headers.keySet();
	}

	@Override
	public void setContentLengthLong(long len) {
		// TODO Auto-generated method stub
	}

}
