package net.etalia.crepuscolo.auth;

import java.io.IOException;
import java.nio.charset.Charset;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.services.CreationService;
import net.etalia.crepuscolo.services.StorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configurable
public class AuthCheckHttpMessageConverter extends AbstractHttpMessageConverter<BaseEntity> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");	
	
	@Autowired(required=true)
	private CreationService creator = null;

	@Autowired(required=false)
	private StorageService storage = null;

	public AuthCheckHttpMessageConverter() {
		super(new MediaType("application", "authcheck", DEFAULT_CHARSET));
	}

	public void setCreationService(CreationService cs) {
		this.creator = cs;
	}

	public void setStorageService(StorageService storage) {
		this.storage = storage;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		//return Persistent.class.isAssignableFrom(clazz);
		return true;
	}

	@Override
	protected BaseEntity readInternal(Class<? extends BaseEntity> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		if (!BaseEntity.class.isAssignableFrom(clazz)) return null;
		String id = (String) RequestContextHolder.getRequestAttributes().getAttribute("_ID_", RequestAttributes.SCOPE_REQUEST);
		if (id != null) {
			RequestContextHolder.getRequestAttributes().removeAttribute("_ID_", RequestAttributes.SCOPE_REQUEST);
			if (storage != null)
				return storage.load(clazz, id);
			if (creator != null)
				try {
					return creator.getEmptyInstance(id);
				} catch (Exception e) {
					// SG Could happen, if the class the method expect is an abstract class
				}
			return null;
		}
		if (creator != null) {
			try {
				return creator.newInstance(clazz);
			} catch (Exception e) {
				// SG Could happen, if the class the method expect is an abstract class
			}
		}
		return null;
	}

	@Override
	protected void writeInternal(BaseEntity t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
	}

}
