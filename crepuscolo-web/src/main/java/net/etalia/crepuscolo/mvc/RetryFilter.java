package net.etalia.crepuscolo.mvc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.OptimisticLockException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;

import net.etalia.crepuscolo.utils.BufferedRequestWrapper;
import net.etalia.crepuscolo.utils.BufferedResponseWrapper;
import net.etalia.crepuscolo.utils.RetryException;

public class RetryFilter implements Filter {

	protected Logger log = Logger.getLogger(RetryFilter.class.getName());

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// By default do not retry
		int retry = 0;
		// Save the exception here, it will be rethrown if retry is not permitted or does not work out
		Throwable exception = null;
		BufferedRequestWrapper breq = new BufferedRequestWrapper((HttpServletRequest) request);
		BufferedResponseWrapper prevresp = null;
		do {
			// Decrement the number of retries still available
			// note that first time this will bring retry to -1
			retry--;
			try {
				if (log.isLoggable(Level.FINE)) {
					if (retry < 0) {
						log.fine("First try");
					} else {
						log.fine("Retries left " + retry);
					}
				}
				// Try to execute the request normally
				request.removeAttribute("retryException");
				prevresp = null;
				BufferedResponseWrapper nresp = new BufferedResponseWrapper((HttpServletResponse)response);
				chain.doFilter(breq, nresp);
				prevresp = nresp;
				Object retExcObj = request.getAttribute("retryException");
				if (retExcObj != null && retExcObj instanceof Throwable) {
					throw (Throwable)retExcObj;
				}
				// If it manages to execute, simply return
				nresp.commit();
				log.fine("No exception, no need to retry");
				return;
			} catch (Throwable e) {
				log.warning("Got exception, checking for retry " + e.getMessage());
				
				// Save the exception 
				exception = e;
				
				// Search in the chain of exceptions for a valid retry candidate
				Throwable found = null;
				Throwable ac = e;
				do {
					if (
							ac instanceof RetryException || 
							ac instanceof OptimisticLockException ||
							ac instanceof TransientDataAccessException ||
							ac instanceof DataIntegrityViolationException
						) {
						found = ac;
						break;
					}
					ac = ac.getCause();
				} while (ac != null);
				
				// Check if an exception worth retrying has been found in the chain
				if (found != null) {
					log.fine("Found exception worth retrying " + found.getClass().getName());
					// If the response has already been committed, there is nothing we can do except rethrow the exception
					if (response.isCommitted()) {
						log.warning("Response was already committed, cannot retry " + found.getClass().getName());
						break;
					}
					// If we are at retry = -1 (first time it executes)
					if (retry < 0) {
						// By default retry 3 times
						retry = 3;
						// If it is an instance of RetryException, check if a number of retries has been specified
						if (found instanceof RetryException) {
							retry = ((RetryException)found).getRetries();
						}
						log.fine("Will retry " + retry + " times");
					}
					// Sleep for a while, to allow race condition to go away
					try {
						// By default sleep 100ms
						long sleep = 100;
						// If it is an instance of RetryException, use the sleep period specified in it
						if (found instanceof RetryException) {
							sleep = ((RetryException)found).getSleep();
						} 
						log.fine("Sleeping " + sleep + " before retry");
						Thread.sleep(sleep);
					} catch (InterruptedException ie) {
						// We have been interrupted, quit
						Thread.currentThread().interrupt();
						return;
					}
				} else {
					// It is not an exception we should retry, quite the cycle and rethrow it
					break;
				}
			}
			// Keep trying as long as specified (and as long as an exception worth retrying is thrown)
		} while (retry > 0);
		// If there was an error payload, send it
		if (prevresp != null) {
			prevresp.commit();
		} else if (exception != null) {
			// Rethrow the exception wrapped in a ServletException
			throw new ServletException(exception);
		}
	}

	@Override
	public void destroy() {
	}

}
