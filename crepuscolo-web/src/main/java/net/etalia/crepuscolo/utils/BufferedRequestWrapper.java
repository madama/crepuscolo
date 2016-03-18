package net.etalia.crepuscolo.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

public class BufferedRequestWrapper extends HttpServletRequestWrapper {

	private byte[] data = null;
	
	public BufferedRequestWrapper(HttpServletRequest request) {
		super(request);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (data == null) {
			ServletInputStream origInput = getRequest().getInputStream();
			data = IOUtils.toByteArray(origInput);
		}
		final ByteArrayInputStream input = new ByteArrayInputStream(data);
		return new ServletInputStream() {
			@Override
			public int read() throws IOException {
				return input.read();
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
