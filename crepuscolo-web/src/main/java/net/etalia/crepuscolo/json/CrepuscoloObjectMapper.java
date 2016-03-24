package net.etalia.crepuscolo.json;

import java.util.Map;

import javax.annotation.PostConstruct;

import net.etalia.crepuscolo.check.CheckerFactory;
import net.etalia.crepuscolo.domain.Entities;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.services.CreationService;
import net.etalia.crepuscolo.services.CreationServiceImpl;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.jalia.DefaultOptions;
import net.etalia.jalia.JsonContext;
import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.OutField;
import net.etalia.jalia.TypeUtil;
import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configurable
public class CrepuscoloObjectMapper extends ObjectMapper {	
	
	private StorageService storageService;
	private CreationService creationService = new CreationServiceImpl();
	private CheckerFactory checkerFactory;
	private boolean client;

	public CrepuscoloObjectMapper(boolean client) {
		this.client = client;
		JaliaDomainFactory factory = new JaliaDomainFactory();
		super.setEntityFactory(factory);
		super.setEntityNameProvider(factory);
		super.setClassDataFactory(factory);
	}
	
	public void setClient(boolean client) {
		this.client = client;
	}
	
	public CrepuscoloObjectMapper() {
		this(false);
	}
	
	@Autowired(required=false)
	public void setStorageService(StorageService storageService) {
		this.storageService = storageService;
	}
	@Autowired(required=false)
	public void setCreationService(CreationService creationService) {
		this.creationService = creationService;
	}
	@Autowired(required=false)
	public void setCheckerFactory(CheckerFactory checkerFactory) {
		this.checkerFactory = checkerFactory;
	}
	
	public void setUnroll(boolean unroll) {
		super.setOption(DefaultOptions.UNROLL_OBJECTS, unroll);
	}
	
	public void addMapping(Class<?> clazz, String name) {
		JaliaDomainFactory factory = (JaliaDomainFactory) super.getEntityFactory();
		factory.map(clazz, name);
	}
	
	public void setAddedMappings(Map<Class<?>, String> mappings) {
		for (Map.Entry<Class<?>, String> entry : mappings.entrySet()) {
			addMapping(entry.getKey(), entry.getValue());
		}
	}
	
	
	/**
	 * Perform default configuration
	 */
	@Override
	@PostConstruct
	public void init() {
		if (super.inited) return;
		JaliaDomainFactory factory = (JaliaDomainFactory) super.getEntityFactory();
		if (!client) {
			factory.setCheckerFactory(checkerFactory);
			factory.setStorageService(storageService);
		} else {
			//setOption(DefaultOptions.INCLUDE_NULLS, true);
			//setOption(DefaultOptions.INCLUDE_EMPTY, true);
		}
		factory.setClient(client);
		factory.setCreationService(creationService);

		
		super.init();
	}
	
	@Override
	protected JsonReader configureReader(JsonReader reader) {
		JsonReader ret = super.configureReader(reader);
		// Set the reader lenient, we want to accept some errors
		ret.setLenient(true);
		return ret;
	}
	
	@Override
	public Object readValue(JsonReader jsonIn, Object pre, TypeUtil hint) {
		init();		
		// Check if we need to start a transaction or not
		if (this.storageService != null) {
			return readValueTransactional(jsonIn, pre, hint);
		} else {
			return super.readValue(jsonIn, pre, hint);
		}
	}
	
	@Transactional(readOnly=true)
	public Object readValueTransactional(JsonReader jsonIn, Object pre, TypeUtil hint) {
		if (pre == null) {
			// Use the _ID_ attribute set by IdPathRequestBody and IdPathRequestBodyProcessor
			if (RequestContextHolder.getRequestAttributes() != null) {
				String id = (String) RequestContextHolder.getRequestAttributes().getAttribute("_ID_", RequestAttributes.SCOPE_REQUEST);
				if (id != null) {
					Class<?> clazz = Entities.getDomainClassByID(id);
					pre = storageService.load((Class<BaseEntity>)clazz, id);
					RequestContextHolder.getRequestAttributes().removeAttribute("_ID_", RequestAttributes.SCOPE_REQUEST);
				}
			}
		}
		return super.readValue(jsonIn, pre, hint);
	}
	
	@Override
	public Object readValue(JsonContext ctx, Object pre, TypeUtil hint) {
		try {
			return super.readValue(ctx, pre, hint);
		} catch (Throwable e) {
			Throwable orig = e;
			while (e != null) {
				if (e instanceof DontModifyException) {
					// Clean up the input
					JsonReader input = ctx.getInput();
					try {
						JsonToken peek = input.peek();
						if (peek == JsonToken.NAME) {
							// We are in an object, skip everything 
							while (input.hasNext()) {
								input.nextName();
								input.skipValue();
							}
							input.endObject();
						} else {
							throw new IllegalStateException("Incorrect state after DontModifyException, peeked " + peek.name());
						}
					} catch (Throwable t) {
						throw new IllegalStateException("Problem cleaning up after DontModifyException", t);
					}
					return ((DontModifyException) e).getEntity();
				}
				e = e.getCause();
			}
			if (orig instanceof RuntimeException) throw (RuntimeException)orig;
			throw new IllegalStateException(orig);
		}
	}
	
	
	@Override
	public void writeValue(JsonWriter jsonOut, OutField fields, Object obj) {
		init();
		// Check if we need to start a transaction or not
		if (this.storageService != null) {
			writeValueTransactional(jsonOut, fields, obj);
		} else {
			super.writeValue(jsonOut, fields, obj);
		}
	}
	
	@Transactional(readOnly=true)
	public void writeValueTransactional(JsonWriter jsonOut, OutField fields, Object obj) {
		super.writeValue(jsonOut, fields, obj);
	}
	
}
