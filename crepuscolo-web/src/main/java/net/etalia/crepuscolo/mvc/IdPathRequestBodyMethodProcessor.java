package net.etalia.crepuscolo.mvc;

import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

public class IdPathRequestBodyMethodProcessor extends RequestResponseBodyMethodProcessor {

	public IdPathRequestBodyMethodProcessor(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(IdPathRequestBody.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		String name = parameter.getParameterAnnotation(IdPathRequestBody.class).value();
		Map<String, String> uriTemplateVars = 
				(Map<String, String>) webRequest.getAttribute(
						HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		String value = uriTemplateVars.get(name);
		webRequest.setAttribute("_ID_", value, RequestAttributes.SCOPE_REQUEST);
		return super.resolveArgument(new IdPathMethodParameter(parameter), mavContainer, webRequest, binderFactory);
	}

}
