package net.etalia.crepuscolo.auth;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.BeanDescriptor;

import net.etalia.crepuscolo.check.CheckAspect;
import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.utils.BufferedResponseWrapper;
import net.etalia.jalia.ObjectMapper;

import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class AuthCheckServlet extends HttpServlet {

	private Validator validator = null; 
	
	@Override
	public void init() throws ServletException {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getMethod().equals("OPTIONS")) return;
		CheckAspect aspect = CheckAspect.aspectOf();
		try {
			aspect.probing(true);
			ObjectMapper mapper = new CrepuscoloObjectMapper();
			String check = req.getParameter("check");
			String clazz = req.getParameter("class");
			if (check != null) {
				List<String> tocheck = mapper.readValue(check, List.class);
				
				ServletContext sctx = req.getServletContext();
		
				List<AuthCheckResult> ret = new ArrayList<AuthCheckResult>();
				
				for (String urlDef : tocheck) {
					BufferedResponseWrapper wresp = null;
					try {
						String[] split = urlDef.split(" ", 2);
						
						RequestDispatcher dispatcher = sctx.getRequestDispatcher(split[1]);
						
						AuthCheckServletRequest wreq = new AuthCheckServletRequest(req, split[0], split[1]);
						if (!split[0].equals("GET")) wreq.overrideHeader("Content-Type", "application/authcheck; charset=utf-8");
						if (req.getHeader("Accept") == null) {
							wreq.overrideHeader("Accept", "application/json, text/html, text/*, */*");
						}
						
						wresp = new BufferedResponseWrapper(resp);
						
						dispatcher.include(wreq, wresp);
					} catch (Throwable e) {
						e.printStackTrace();
						ret.add(new AuthCheckResult(urlDef, 500));
						continue;
					}
					if (wresp.getStatus() >= 200 && wresp.getStatus() < 300) {
						throw new IllegalStateException("Called " + urlDef + " for auth check, but it has no auth checks");
					} else {
						ret.add(new AuthCheckResult(urlDef, wresp.getStatus()));
					}
				}
	
				resp.setContentType("application/json");
				mapper.writeValue(resp.getOutputStream(), ret);
			} else if (clazz != null) {
				// TODO implement some type of cache here
				try {
					if (clazz.indexOf('.') == -1) {
						// we serialize the @entity only with the class name, so
						// we should accept it as is
						clazz = "net.etalia.crepuscolo.domain." + clazz; //TODO: this sould be a param
					}
					Class rclazz = Class.forName(clazz);
					BeanDescriptor descr = validator.getConstraintsForClass(rclazz);
					BeanInfo beanInfo = Introspector.getBeanInfo(rclazz);
					PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
					Map<String, PropertyValidationDescriptor> outpds = new HashMap<String, PropertyValidationDescriptor>();
					for (PropertyDescriptor pd : pds) {
						PropertyValidationDescriptor pvad = new PropertyValidationDescriptor(rclazz, null, pd, descr.getConstraintsForProperty(pd.getName()));
						if (!pvad.isEmpty()) outpds.put(pd.getName(), pvad);
					}
					resp.setContentType("application/json");
					mapper.writeValue(resp.getOutputStream(), outpds);
				} catch (Exception e) {
					throw new ServletException(e);
				}
			}
		} finally {
			aspect.probing(false);
		}
	}
	
}
